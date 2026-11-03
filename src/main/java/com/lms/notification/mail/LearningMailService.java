package com.lms.notification.mail;

import com.lms.assessment.entity.Assessment;
import com.lms.common.mail.MailMessage;
import com.lms.common.mail.MailSender;
import com.lms.config.AppProperties;
import com.lms.course.entity.Course;
import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionReminderKind;
import com.lms.resource.entity.CampusResource;
import com.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Composes and sends the learning notification mails: what a student is given
 * (a course, an assessment, a live class, a toolkit resource) and when a live
 * class is about to start.
 *
 * <p>Every method is {@link Async}. These sends fan out to the whole student
 * body in the assessment and toolkit cases, and a synchronous SMTP round trip
 * per student would hold the publishing request open for as long as that takes.
 * A failure is logged and swallowed for the same reason: the thing being
 * announced is already committed, and a mail server having a bad minute must
 * not undo it. Callers reach these methods through
 * {@link LearningMailEventListener} after the commit, never inline.
 *
 * <p>Bodies are plain text, matching the invitation and reset mails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningMailService {

    // Pinned to English rather than the JVM default: the server's locale
    // decides day and month names and the case of am/pm, so an unpinned
    // formatter makes the same mail read differently on another machine.
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy 'at' h:mm a", Locale.ENGLISH);

    private final MailSender mailSender;
    private final AppProperties appProperties;
    private final LearningMailProperties properties;

    @Async
    public void sendEnrolledInCourse(User student, Course course) {
        if (skip("course enrolment", student)) return;

        send(student, "You've been enrolled in " + course.getTitle(), """
                Hello %s,

                You now have access to %s.%s

                Open the course: %s

                Happy learning,
                %s
                """.formatted(
                student.getName(),
                course.getTitle(),
                paragraph(course.getDescription()),
                appProperties.coursePlayerLink(course.getId()),
                properties.getSignature()));
    }

    @Async
    public void sendAssessmentPublished(List<User> students, Assessment assessment) {
        String window = assessmentWindow(assessment);
        for (User student : students) {
            if (skip("assessment", student)) continue;

            send(student, "New assessment available: " + assessment.getTitle(), """
                    Hello %s,

                    A new assessment has been published for you.

                    Assessment: %s
                    Duration:   %d minutes
                    Total marks: %d
                    Attempts allowed: %d%s

                    Start when you're ready: %s

                    Good luck,
                    %s
                    """.formatted(
                    student.getName(),
                    assessment.getTitle(),
                    assessment.getDurationMinutes(),
                    assessment.getTotalMarks(),
                    assessment.getMaxAttempts(),
                    window,
                    appProperties.assessmentLink(assessment.getId()),
                    properties.getSignature()));
        }
    }

    @Async
    public void sendLiveSessionScheduled(List<User> students, LiveSession session, String courseTitle) {
        for (User student : students) {
            if (skip("live class", student)) continue;

            send(student, "Live class scheduled: " + session.getTitle(), """
                    Hello %s,

                    A live class has been added to your calendar.

                    Class:  %s
                    Course: %s
                    When:   %s
                    Length: %d minutes

                    It will appear in your calendar, and you can join from: %s

                    See you there,
                    %s
                    """.formatted(
                    student.getName(),
                    session.getTitle(),
                    courseTitle,
                    formatWhen(session.getScheduledStart()),
                    Duration.between(session.getScheduledStart(), session.getScheduledEnd()).toMinutes(),
                    appProperties.liveClassesLink(),
                    properties.getSignature()));
        }
    }

    @Async
    public void sendLiveSessionReminder(List<User> students, LiveSession session, String courseTitle,
                                        LiveSessionReminderKind kind) {
        boolean imminent = kind == LiveSessionReminderKind.FIFTEEN_MINUTES;
        // Neutral wording for the early reminder: its window runs from 24 hours
        // out down to two hours out, so "tomorrow" would be wrong for a class
        // later the same day. The body carries the exact date and time.
        String subject = imminent
                ? "Starting in 15 minutes: " + session.getTitle()
                : "Reminder: " + session.getTitle();
        // The imminent reminder points straight at the room; a day out the room
        // is not open yet, so that one points at the class list instead.
        String link = imminent
                ? appProperties.liveSessionRoomLink(session.getId())
                : appProperties.liveClassesLink();
        String lead = imminent
                ? "Your live class starts in about 15 minutes."
                : "This is a reminder about your upcoming live class.";

        for (User student : students) {
            if (skip("live class reminder", student)) continue;

            send(student, subject, """
                    Hello %s,

                    %s

                    Class:  %s
                    Course: %s
                    When:   %s

                    %s %s

                    %s
                    """.formatted(
                    student.getName(),
                    lead,
                    session.getTitle(),
                    courseTitle,
                    formatWhen(session.getScheduledStart()),
                    imminent ? "Join here:" : "Details:",
                    link,
                    properties.getSignature()));
        }
    }

    @Async
    public void sendCampusResourcePublished(List<User> students, CampusResource resource) {
        for (User student : students) {
            if (skip("campus toolkit", student)) continue;

            send(student, "New in the campus toolkit: " + resource.getTitle(), """
                    Hello %s,

                    A new resource has been added to your campus toolkit.

                    Resource: %s
                    Category: %s
                    File:     %s (%s)%s

                    Find it on your dashboard: %s

                    %s
                    """.formatted(
                    student.getName(),
                    resource.getTitle(),
                    resource.getCategory(),
                    resource.getFileName(),
                    resource.getFileSize(),
                    paragraph(resource.getDescription()),
                    appProperties.campusToolkitLink(),
                    properties.getSignature()));
        }
    }

    // --- Internal helpers ---

    private void send(User student, String subject, String body) {
        try {
            mailSender.send(new MailMessage(student.getEmail(), subject, body));
        } catch (Exception ex) {
            // Swallowed on purpose: see the class comment.
            log.warn("Could not mail \"{}\" to {}: {}", subject, student.getEmail(), ex.getMessage());
        }
    }

    /** True when there is no point mailing this student. */
    private boolean skip(String what, User student) {
        if (student == null) {
            return true;
        }
        if (!student.isActive()) {
            log.debug("Skipping {} mail for inactive user {}", what, student.getId());
            return true;
        }
        if (student.getEmail() == null || student.getEmail().isBlank()) {
            log.warn("Skipping {} mail: user {} has no email address", what, student.getId());
            return true;
        }
        return false;
    }

    private String formatWhen(Instant instant) {
        return WHEN.format(instant.atZone(ZoneId.of(properties.getTimeZone())))
                + " (" + properties.getTimeZoneLabel() + ")";
    }

    /** A description as its own paragraph, or nothing when there is none. */
    private String paragraph(String text) {
        return (text == null || text.isBlank()) ? "" : "\n\n" + text.trim();
    }

    private String assessmentWindow(Assessment assessment) {
        if (assessment.getStartTime() == null && assessment.getEndTime() == null) {
            return "";
        }
        if (assessment.getStartTime() != null && assessment.getEndTime() != null) {
            return "\nOpen from: " + formatWhen(assessment.getStartTime())
                    + "\nCloses:    " + formatWhen(assessment.getEndTime());
        }
        return assessment.getStartTime() != null
                ? "\nOpen from: " + formatWhen(assessment.getStartTime())
                : "\nCloses:    " + formatWhen(assessment.getEndTime());
    }
}
