package com.lms.notification.mail;

import com.lms.common.mail.MailMessage;
import com.lms.common.mail.MailSender;
import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import com.lms.course.repository.CourseRepository;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.enrollment.event.EnrollmentCreatedEvent;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionStatus;
import com.lms.live.event.LiveSessionScheduledEvent;
import com.lms.live.repository.LiveSessionRepository;
import com.lms.role.entity.Role;
import com.lms.role.repository.RoleRepository;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Drives the learning notification mails through the real Spring context.
 *
 * <p>Deliberately not {@code @Transactional}: the listeners fire on
 * AFTER_COMMIT, so a test that rolled back would prove nothing. Everything here
 * commits for real through a {@link TransactionTemplate} and is cleared down
 * afterwards. The test profile runs on in-memory H2, so no real data is touched.
 *
 * <p>The sends are {@code @Async}, hence the timeout on each verify: the mail
 * leaves on another thread shortly after the commit returns.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("learning notification mails, end to end")
class LearningMailIntegrationTest {

    @MockBean
    private MailSender mailSender;

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private CourseRepository courses;
    @Autowired private EnrollmentRepository enrollments;
    @Autowired private LiveSessionRepository liveSessions;

    private User instructor;
    private User enrolledOne;
    private User enrolledTwo;
    private Course course;

    /**
     * Seeds inside one transaction. Saving each row separately would leave the
     * shared Role detached, and the cascade from User would then try to
     * persist it a second time.
     */
    @BeforeEach
    void seed() {
        commit(() -> {
            instructor = saveUser("Ada", "ada@example.com", "INSTRUCTOR", true);
            enrolledOne = saveUser("Asha", "asha@example.com", "STUDENT", true);
            enrolledTwo = saveUser("Bo", "bo@example.com", "STUDENT", true);
            User enrolledButInactive = saveUser("Pending", "pending@example.com", "STUDENT", false);
            saveUser("Outsider", "outsider@example.com", "STUDENT", true);

            course = courses.saveAndFlush(Course.builder()
                    .title("Data Structures")
                    .description("Trees, graphs and the rest.")
                    .status(CourseStatus.PUBLISHED)
                    .createdBy(instructor.getId())
                    .instructorId(instructor.getId())
                    .build());

            enroll(enrolledOne, course);
            enroll(enrolledTwo, course);
            enroll(enrolledButInactive, course);
        });
    }

    @AfterEach
    void tidy() {
        commit(() -> {
            liveSessions.deleteAll();
            enrollments.deleteAll();
            courses.deleteAll();
            users.deleteAll();
        });
    }

    @Test
    @DisplayName("an enrolment mails that one student, with a link into the course")
    void enrolmentMailsTheStudent() {
        commit(() -> eventPublisher.publishEvent(
                new EnrollmentCreatedEvent(enrolledOne.getId(), course.getId())));

        ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailSender, timeout(5_000)).send(captor.capture());

        MailMessage mail = captor.getValue();
        assertThat(mail.getTo()).isEqualTo("asha@example.com");
        assertThat(mail.getSubject()).contains("Data Structures");
        assertThat(mail.getBody())
                .contains("Hello Asha")
                .contains("/learn/" + course.getId() + "/player");
    }

    @Test
    @DisplayName("a scheduled live class mails the enrolled students, and only those")
    void liveClassMailsEnrolledStudentsOnly() {
        LiveSession session = saveSession(course, "Graph algorithms walkthrough");

        commit(() -> eventPublisher.publishEvent(
                new LiveSessionScheduledEvent(session.getId(), course.getId())));

        ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailSender, timeout(5_000).times(2)).send(captor.capture());

        List<String> recipients = captor.getAllValues().stream().map(MailMessage::getTo).toList();
        assertThat(recipients).containsExactlyInAnyOrder("asha@example.com", "bo@example.com");
        // The deactivated enrolment and the student on no enrolment at all are
        // both outside the audience.
        assertThat(recipients)
                .doesNotContain("pending@example.com")
                .doesNotContain("outsider@example.com");

        assertThat(captor.getAllValues().get(0).getBody())
                .contains("Graph algorithms walkthrough")
                .contains("Data Structures")
                .contains("/learn/live-classes");
    }

    @Test
    @DisplayName("a class on a course nobody is enrolled on sends nothing")
    void noEnrolmentsMeansNoMail() {
        LiveSession session = commitAndReturn(() -> {
            Course empty = courses.saveAndFlush(Course.builder()
                    .title("Unattended Course")
                    .status(CourseStatus.PUBLISHED)
                    .createdBy(instructor.getId())
                    .instructorId(instructor.getId())
                    .build());
            return newSession(empty, "Empty class");
        });

        commit(() -> eventPublisher.publishEvent(
                new LiveSessionScheduledEvent(session.getId(), session.getCourse().getId())));

        verify(mailSender, timeout(2_000).times(0)).send(any());
    }

    @Test
    @DisplayName("a rolled-back change announces nothing")
    void rollbackSendsNothing() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(
                    new EnrollmentCreatedEvent(enrolledOne.getId(), course.getId()));
            // The enrolment never happened, so neither should the mail.
            status.setRollbackOnly();
        });

        verify(mailSender, timeout(2_000).times(0)).send(any());
    }

    // --- helpers ---

    private void commit(Runnable work) {
        transactionTemplate.executeWithoutResult(status -> work.run());
    }

    private <T> T commitAndReturn(java.util.function.Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }

    private LiveSession saveSession(Course on, String title) {
        return commitAndReturn(() -> newSession(on, title));
    }

    private LiveSession newSession(Course on, String title) {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        return liveSessions.saveAndFlush(LiveSession.builder()
                .tenantId(UUID.randomUUID())
                .course(on)
                .instructor(instructor)
                .title(title)
                .scheduledStart(start)
                .scheduledEnd(start.plus(1, ChronoUnit.HOURS))
                .roomName("room-" + UUID.randomUUID())
                .sessionMode("livekit")
                .status(LiveSessionStatus.SCHEDULED)
                .build());
    }

    private User saveUser(String name, String email, String roleName, boolean active) {
        Role role = roles.findByName(roleName)
                .orElseGet(() -> roles.save(Role.builder().name(roleName).description(roleName).build()));
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("not-used-here");
        user.setActive(active);
        user.assignRole(role, null);
        return users.saveAndFlush(user);
    }

    private void enroll(User student, Course on) {
        enrollments.saveAndFlush(Enrollment.builder()
                .student(student)
                .course(on)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .build());
    }
}
