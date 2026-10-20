package com.lms.live.service;

import com.lms.common.exception.ApplicationException;
import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import com.lms.course.repository.CourseRepository;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.live.dto.CourseAttendanceSummary;
import com.lms.live.dto.MyAttendanceResponse;
import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionAttendance;
import com.lms.live.entity.LiveSessionStatus;
import com.lms.live.repository.LiveSessionAttendanceRepository;
import com.lms.live.repository.LiveSessionRepository;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LiveAttendanceReportTest {

    @Autowired LiveAttendanceReportService reports;
    @Autowired LiveSessionRepository sessions;
    @Autowired LiveSessionAttendanceRepository attendance;
    @Autowired CourseRepository courses;
    @Autowired EnrollmentRepository enrollments;
    @Autowired UserRepository users;

    private User instructor;
    private User ada;
    private User grace;
    private Course course;
    private LiveSession first;
    private LiveSession second;

    @BeforeEach
    void seed() {
        instructor = user("Teacher");
        ada = user("Ada");
        grace = user("Grace");
        course = courses.save(Course.builder().title("Live course").createdBy(instructor.getId())
                .instructorId(instructor.getId()).status(CourseStatus.PUBLISHED).build());
        enrollments.save(Enrollment.builder().student(ada).course(course).build());
        enrollments.save(Enrollment.builder().student(grace).course(course).build());

        Instant base = Instant.now().minus(Duration.ofDays(3));
        first = session(LiveSessionStatus.ENDED, base);
        second = session(LiveSessionStatus.ENDED, base.plus(Duration.ofDays(1)));
        session(LiveSessionStatus.MISSED, base.plus(Duration.ofDays(2)));   // never held: not counted
        session(LiveSessionStatus.SCHEDULED, Instant.now().plus(Duration.ofDays(2))); // future: not counted

        attend(first, ada, 50);
        attend(second, ada, 30);
        attend(second, ada, 15);      // rejoined: minutes add up, still one class
        attend(first, grace, 40);
        attend(first, instructor, 60); // the instructor is not a student
    }

    @Test
    void courseSummaryCountsOnlyHeldClassesAndEnrolledStudents() {
        CourseAttendanceSummary summary = reports.courseSummary(course.getId(), asInstructor());

        assertThat(summary.classesHeld()).isEqualTo(2);
        assertThat(summary.enrolledStudents()).isEqualTo(2);
        assertThat(summary.studentsAttendedAny()).isEqualTo(2);
        assertThat(summary.attendanceRate()).isEqualTo(75.0); // (100% + 50%) / 2

        CourseAttendanceSummary.StudentRow adaRow = row(summary, ada);
        assertThat(adaRow.classesAttended()).isEqualTo(2);
        assertThat(adaRow.percentage()).isEqualTo(100.0);
        assertThat(adaRow.minutesAttended()).isEqualTo(95);

        CourseAttendanceSummary.StudentRow graceRow = row(summary, grace);
        assertThat(graceRow.classesAttended()).isEqualTo(1);
        assertThat(graceRow.percentage()).isEqualTo(50.0);
        assertThat(summary.students()).extracting(CourseAttendanceSummary.StudentRow::studentId)
                .doesNotContain(instructor.getId());

        assertThat(summary.classes()).extracting(CourseAttendanceSummary.ClassRow::studentsAttended)
                .containsExactly(1, 2); // newest first: second (Ada), first (Ada + Grace)
    }

    @Test
    void timeLeftRunningLongAfterTheClassIsNotCounted() {
        User late = user("Late");
        enrollments.save(Enrollment.builder().student(late).course(course).build());
        attend(first, late, Duration.ofDays(3).toMinutes()); // class was ended by hand days later

        CourseAttendanceSummary summary = reports.courseSummary(course.getId(), asInstructor());

        assertThat(row(summary, late).minutesAttended()).isEqualTo(60); // the 1-hour class itself
    }

    @Test
    void otherInstructorsCannotSeeTheCourse() {
        LmsUserDetails stranger = LmsUserDetails.fromClaims(UUID.randomUUID(), "x@lms.test",
                Set.of("INSTRUCTOR"), Set.of(), UUID.randomUUID());

        assertThatThrownBy(() -> reports.courseSummary(course.getId(), stranger))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    void adminsCanSeeAnyCourse() {
        LmsUserDetails admin = LmsUserDetails.fromClaims(UUID.randomUUID(), "admin@lms.test",
                Set.of("ADMIN"), Set.of(), UUID.randomUUID());

        assertThat(reports.courseSummary(course.getId(), admin).classesHeld()).isEqualTo(2);
    }

    @Test
    void aStudentSeesTheirOwnAttendance() {
        MyAttendanceResponse mine = reports.myAttendance(grace.getId());

        assertThat(mine.classesHeld()).isEqualTo(2);
        assertThat(mine.classesAttended()).isEqualTo(1);
        assertThat(mine.percentage()).isEqualTo(50.0);
        assertThat(mine.minutesAttended()).isEqualTo(40);
        assertThat(mine.classes()).extracting(MyAttendanceResponse.ClassRow::attended)
                .containsExactly(false, true);
    }

    @Test
    void aStudentWithNoCoursesGetsAnEmptyReport() {
        MyAttendanceResponse mine = reports.myAttendance(user("Nobody").getId());

        assertThat(mine.classesHeld()).isZero();
        assertThat(mine.classes()).isEmpty();
    }

    private User user(String name) {
        return users.save(User.builder().name(name).email(UUID.randomUUID() + "@lms.test")
                .password("unused").active(true).build());
    }

    private LiveSession session(LiveSessionStatus status, Instant start) {
        return sessions.save(LiveSession.builder()
                .tenantId(UUID.randomUUID())
                .course(course)
                .instructor(instructor)
                .title("Class " + start)
                .scheduledStart(start)
                .scheduledEnd(start.plus(Duration.ofHours(1)))
                .actualStart(status == LiveSessionStatus.ENDED ? start : null)
                .roomName("room-" + UUID.randomUUID())
                .status(status)
                .build());
    }

    private void attend(LiveSession session, User who, long minutes) {
        Instant joined = session.getScheduledStart();
        attendance.save(LiveSessionAttendance.builder().session(session).user(who).joinedAt(joined)
                .leftAt(joined.plus(Duration.ofMinutes(minutes))).durationSeconds(minutes * 60).build());
    }

    private LmsUserDetails asInstructor() {
        return LmsUserDetails.fromClaims(instructor.getId(), instructor.getEmail(),
                Set.of("INSTRUCTOR"), Set.of(), UUID.randomUUID());
    }

    private static CourseAttendanceSummary.StudentRow row(CourseAttendanceSummary summary, User who) {
        return summary.students().stream().filter(r -> r.studentId().equals(who.getId())).findFirst().orElseThrow();
    }
}
