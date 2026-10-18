package com.lms.course;

import com.lms.course.controller.LearningController;
import com.lms.course.dto.request.LearningHeartbeatRequest;
import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import com.lms.course.repository.CourseRepository;
import com.lms.course.service.LearningTimeService;
import com.lms.common.exception.ApplicationException;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LearningTimeIntegrationTest {

    private static final Instant NOON = Instant.parse("2026-10-14T12:00:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 14);

    @Autowired LearningTimeService service;
    @Autowired LearningController controller;
    @Autowired UserRepository users;
    @Autowired CourseRepository courses;
    @Autowired EnrollmentRepository enrollments;

    private UUID studentId;
    private UUID courseId;

    @BeforeEach
    void seed() {
        User student = users.save(User.builder().name("Timed student")
                .email(UUID.randomUUID() + "@lms.test").password("unused").active(true).build());
        studentId = student.getId();
        Course course = courses.save(Course.builder().title("Timed course").createdBy(studentId)
                .status(CourseStatus.PUBLISHED).build());
        courseId = course.getId();
        enrollments.save(Enrollment.builder().student(student).course(course).build());
        var principal = LmsUserDetails.fromClaims(studentId, student.getEmail(), Set.of("STUDENT"),
                Set.of("COURSE_VIEW"), UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void heartbeatsAddUpPerDay() {
        service.record(studentId, courseId, 60, NOON);
        service.record(studentId, courseId, 60, NOON.plusSeconds(60));
        service.record(studentId, courseId, 30, NOON.plusSeconds(90));

        assertThat(service.secondsByDay(studentId, DAY, DAY)).containsEntry(DAY, 150L);
    }

    @Test
    void aClientCannotClaimMoreThanTheTimeThatPassed() {
        service.record(studentId, courseId, 60, NOON);
        int credited = service.record(studentId, courseId, 60, NOON.plusSeconds(5));

        assertThat(credited).isEqualTo(5);
        assertThat(service.secondsByDay(studentId, DAY, DAY)).containsEntry(DAY, 65L);
    }

    @Test
    void oneHeartbeatIsCappedEvenAfterALongGap() {
        service.record(studentId, courseId, 60, NOON);
        int credited = service.record(studentId, courseId, 600, NOON.plusSeconds(3600));

        assertThat(credited).isEqualTo(90);
    }

    @Test
    void theControllerRecordsTimeForEnrolledStudents() {
        var body = controller.recordTime(courseId, new LearningHeartbeatRequest(45)).getBody().getData();

        assertThat(body.get("creditedSeconds")).isEqualTo(45);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        assertThat(service.secondsByDay(studentId, today, today)).containsEntry(today, 45L);
    }

    @Test
    void timeIsNotRecordedForCoursesTheStudentIsNotIn() {
        UUID otherCourse = courses.save(Course.builder().title("Other").createdBy(studentId)
                .status(CourseStatus.PUBLISHED).build()).getId();

        assertThatThrownBy(() -> controller.recordTime(otherCourse, new LearningHeartbeatRequest(30)))
                .isInstanceOf(ApplicationException.class);
    }
}
