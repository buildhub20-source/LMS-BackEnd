package com.lms.notification.mail;

import com.lms.assessment.entity.Assessment;
import com.lms.common.mail.MailMessage;
import com.lms.common.mail.MailSender;
import com.lms.config.AppProperties;
import com.lms.course.entity.Course;
import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionReminderKind;
import com.lms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("LearningMailService")
class LearningMailServiceTest {

    /** 2026-11-05 at 15:30 IST, so the rendered local time is unambiguous. */
    private static final Instant START =
            ZonedDateTime.of(2026, 11, 5, 15, 30, 0, 0, ZoneId.of("Asia/Kolkata")).toInstant();

    @Mock
    private MailSender mailSender;

    private LearningMailService service;

    @BeforeEach
    void setUp() {
        AppProperties app = new AppProperties();
        app.setFrontendBaseUrl("http://localhost:3000");
        service = new LearningMailService(mailSender, app, new LearningMailProperties());
    }

    private User student(String name, String email, boolean active) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(name);
        user.setEmail(email);
        user.setActive(active);
        return user;
    }

    private Course course() {
        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setTitle("Data Structures");
        return course;
    }

    private LiveSession liveSession(Course course) {
        LiveSession session = new LiveSession();
        session.setId(UUID.randomUUID());
        session.setTitle("Graph algorithms walkthrough");
        session.setCourse(course);
        session.setScheduledStart(START);
        session.setScheduledEnd(START.plusSeconds(3600));
        return session;
    }

    private MailMessage captureSingleSend() {
        ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("enrolment")
    class Enrolment {

        @Test
        @DisplayName("names the course and links to its player")
        void linksToThePlayer() {
            Course course = course();
            service.sendEnrolledInCourse(student("Asha", "asha@example.com", true), course);

            MailMessage mail = captureSingleSend();
            assertThat(mail.getTo()).isEqualTo("asha@example.com");
            assertThat(mail.getSubject()).contains("Data Structures");
            assertThat(mail.getBody())
                    .contains("Hello Asha")
                    .contains("http://localhost:3000/learn/" + course.getId() + "/player");
        }

        @Test
        @DisplayName("skips a student with no email rather than sending to null")
        void skipsMissingEmail() {
            service.sendEnrolledInCourse(student("Nobody", null, true), course());
            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("skips a deactivated student")
        void skipsInactive() {
            service.sendEnrolledInCourse(student("Gone", "gone@example.com", false), course());
            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("a failing transport does not escape to the caller")
        void swallowsTransportFailure() {
            doThrow(new RuntimeException("smtp down")).when(mailSender).send(any());

            // The enrolment is already committed, so a bad mail server must not
            // surface as a failure on something that has already happened.
            service.sendEnrolledInCourse(student("Asha", "asha@example.com", true), course());

            verify(mailSender).send(any());
        }
    }

    @Nested
    @DisplayName("assessment")
    class AssessmentMail {

        @Test
        @DisplayName("mails every active student and states the rules")
        void mailsAllStudents() {
            Assessment assessment = new Assessment();
            assessment.setId(UUID.randomUUID());
            assessment.setTitle("Midterm");
            assessment.setDurationMinutes(90);
            assessment.setTotalMarks(50);
            assessment.setMaxAttempts(2);

            service.sendAssessmentPublished(List.of(
                    student("Asha", "asha@example.com", true),
                    student("Bo", "bo@example.com", true)), assessment);

            ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
            verify(mailSender, times(2)).send(captor.capture());

            assertThat(captor.getAllValues()).extracting(MailMessage::getTo)
                    .containsExactly("asha@example.com", "bo@example.com");
            assertThat(captor.getAllValues().get(0).getBody())
                    .contains("90 minutes")
                    .contains("Total marks: 50")
                    .contains("Attempts allowed: 2");
        }
    }

    @Nested
    @DisplayName("live class reminders")
    class Reminders {

        @Test
        @DisplayName("the early reminder avoids saying tomorrow, since it can fire the same day")
        void earlyReminderIsDayNeutral() {
            service.sendLiveSessionReminder(
                    List.of(student("Asha", "asha@example.com", true)),
                    liveSession(course()), "Data Structures", LiveSessionReminderKind.DAY_BEFORE);

            MailMessage mail = captureSingleSend();
            assertThat(mail.getSubject()).isEqualTo("Reminder: Graph algorithms walkthrough");
            assertThat(mail.getSubject()).doesNotContainIgnoringCase("tomorrow");
            assertThat(mail.getBody()).doesNotContainIgnoringCase("tomorrow");
            // Far enough out that the room is not open, so it points at the list.
            assertThat(mail.getBody()).contains("http://localhost:3000/learn/live-classes");
        }

        @Test
        @DisplayName("the imminent reminder links straight into the room")
        void imminentReminderLinksToRoom() {
            LiveSession session = liveSession(course());
            service.sendLiveSessionReminder(
                    List.of(student("Asha", "asha@example.com", true)),
                    session, "Data Structures", LiveSessionReminderKind.FIFTEEN_MINUTES);

            MailMessage mail = captureSingleSend();
            assertThat(mail.getSubject()).isEqualTo("Starting in 15 minutes: Graph algorithms walkthrough");
            assertThat(mail.getBody()).contains("http://localhost:3000/live/" + session.getId() + "/room");
        }

        @Test
        @DisplayName("renders the start in the configured zone, not UTC")
        void rendersLocalTime() {
            service.sendLiveSessionReminder(
                    List.of(student("Asha", "asha@example.com", true)),
                    liveSession(course()), "Data Structures", LiveSessionReminderKind.FIFTEEN_MINUTES);

            // 15:30 IST, not the 10:00 UTC that the raw instant would print as.
            assertThat(captureSingleSend().getBody())
                    .contains("3:30 PM (IST)")
                    .contains("Thursday, 5 November 2026");
        }

        @Test
        @DisplayName("an inactive student in the batch is passed over, the rest still get it")
        void skipsInactiveWithinBatch() {
            service.sendLiveSessionReminder(List.of(
                            student("Gone", "gone@example.com", false),
                            student("Asha", "asha@example.com", true)),
                    liveSession(course()), "Data Structures", LiveSessionReminderKind.FIFTEEN_MINUTES);

            assertThat(captureSingleSend().getTo()).isEqualTo("asha@example.com");
        }
    }
}
