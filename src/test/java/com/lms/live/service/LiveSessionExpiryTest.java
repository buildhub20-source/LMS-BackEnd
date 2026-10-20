package com.lms.live.service;

import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import com.lms.course.repository.CourseRepository;
import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionAttendance;
import com.lms.live.entity.LiveSessionStatus;
import com.lms.live.repository.LiveSessionAttendanceRepository;
import com.lms.live.repository.LiveSessionRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LiveSessionExpiryTest {

    @Autowired LiveSessionService service;
    @Autowired LiveSessionRepository sessions;
    @Autowired LiveSessionAttendanceRepository attendance;
    @Autowired CourseRepository courses;
    @Autowired UserRepository users;

    private final UUID tenantId = UUID.randomUUID();
    private Course course;
    private User instructor;
    private User student;

    @BeforeEach
    void seed() {
        instructor = users.save(User.builder().name("Teacher").email(UUID.randomUUID() + "@lms.test")
                .password("unused").active(true).build());
        student = users.save(User.builder().name("Learner").email(UUID.randomUUID() + "@lms.test")
                .password("unused").active(true).build());
        course = courses.save(Course.builder().title("Live course").createdBy(instructor.getId())
                .status(CourseStatus.PUBLISHED).build());
    }

    @Test
    void aClassLeftLiveLongAfterItsEndIsEndedAndAttendanceStopsAtTheScheduledEnd() {
        Instant start = Instant.now().minus(Duration.ofDays(30));
        Instant end = start.plus(Duration.ofHours(1));
        LiveSession forgotten = session(LiveSessionStatus.LIVE, start, end);
        forgotten.setActualStart(start);
        LiveSessionAttendance present = attendance.save(LiveSessionAttendance.builder()
                .session(forgotten).user(student).joinedAt(start.plus(Duration.ofMinutes(5))).build());

        service.expireStaleSessions(tenantId);

        assertThat(forgotten.getStatus()).isEqualTo(LiveSessionStatus.ENDED);
        assertThat(forgotten.getActualEnd()).isEqualTo(end);
        assertThat(present.getLeftAt()).isEqualTo(end);
        assertThat(present.getDurationSeconds()).isEqualTo(Duration.ofMinutes(55).getSeconds());
    }

    @Test
    void aClassThatRunsOverStaysLiveWithinTheGracePeriod() {
        Instant end = Instant.now().minus(Duration.ofMinutes(30));
        LiveSession overrunning = session(LiveSessionStatus.LIVE, end.minus(Duration.ofHours(1)), end);

        service.expireStaleSessions(tenantId);

        assertThat(overrunning.getStatus()).isEqualTo(LiveSessionStatus.LIVE);
    }

    @Test
    void aScheduledClassWhoseSlotPassedIsMissed() {
        Instant start = Instant.now().minus(Duration.ofDays(2));
        LiveSession neverStarted = session(LiveSessionStatus.SCHEDULED, start, start.plus(Duration.ofHours(1)));

        service.expireStaleSessions(tenantId);

        assertThat(neverStarted.getStatus()).isEqualTo(LiveSessionStatus.MISSED);
    }

    @Test
    void upcomingAndOtherInstitutesClassesAreUntouched() {
        Instant soon = Instant.now().plus(Duration.ofHours(3));
        LiveSession upcoming = session(LiveSessionStatus.SCHEDULED, soon, soon.plus(Duration.ofHours(1)));
        LiveSession elsewhere = session(LiveSessionStatus.SCHEDULED,
                Instant.now().minus(Duration.ofDays(2)), Instant.now().minus(Duration.ofDays(2)).plus(Duration.ofHours(1)));
        elsewhere.setTenantId(UUID.randomUUID());
        sessions.save(elsewhere);

        service.expireStaleSessions(tenantId);

        assertThat(upcoming.getStatus()).isEqualTo(LiveSessionStatus.SCHEDULED);
        assertThat(elsewhere.getStatus()).isEqualTo(LiveSessionStatus.SCHEDULED);
    }

    private LiveSession session(LiveSessionStatus status, Instant start, Instant end) {
        return sessions.save(LiveSession.builder()
                .tenantId(tenantId)
                .course(course)
                .instructor(instructor)
                .title("Session")
                .scheduledStart(start)
                .scheduledEnd(end)
                .roomName("room-" + UUID.randomUUID())
                .status(status)
                .build());
    }
}
