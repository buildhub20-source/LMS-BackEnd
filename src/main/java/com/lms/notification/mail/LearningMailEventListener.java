package com.lms.notification.mail;

import com.lms.assessment.entity.Assessment;
import com.lms.assessment.event.AssessmentPublishedEvent;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.course.entity.Course;
import com.lms.course.repository.CourseRepository;
import com.lms.enrollment.event.EnrollmentCreatedEvent;
import com.lms.live.entity.LiveSession;
import com.lms.live.event.LiveSessionScheduledEvent;
import com.lms.live.repository.LiveSessionRepository;
import com.lms.resource.entity.CampusResource;
import com.lms.resource.event.CampusResourcePublishedEvent;
import com.lms.resource.repository.CampusResourceRepository;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Turns a committed learning change into the mails it should produce.
 *
 * <p>Every listener runs AFTER_COMMIT, so a rolled-back enrolment or a failed
 * publish never announces itself. Each one resolves the audience and hands off
 * to {@link LearningMailService}, which sends asynchronously — the listener
 * itself only reads. {@link LearningMailAudience} decides who each mail goes
 * to.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LearningMailEventListener {

    private final LearningMailService learningMailService;
    private final LearningMailProperties properties;
    private final LearningMailAudience audience;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AssessmentRepository assessmentRepository;
    private final LiveSessionRepository liveSessionRepository;
    private final CampusResourceRepository campusResourceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnrollmentCreated(EnrollmentCreatedEvent event) {
        if (!properties.isEnrollmentEnabled()) return;

        User student = userRepository.findById(event.studentId()).orElse(null);
        Course course = courseRepository.findById(event.courseId()).orElse(null);
        if (student == null || course == null) {
            log.warn("Skipping enrolment mail: student {} or course {} is gone",
                    event.studentId(), event.courseId());
            return;
        }
        learningMailService.sendEnrolledInCourse(student, course);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssessmentPublished(AssessmentPublishedEvent event) {
        if (!properties.isAssessmentEnabled()) return;

        Assessment assessment = assessmentRepository.findById(event.assessmentId()).orElse(null);
        if (assessment == null) {
            log.warn("Skipping assessment mail: assessment {} is gone", event.assessmentId());
            return;
        }
        List<User> students = audience.activeStudents();
        if (students.isEmpty()) return;

        log.info("Mailing assessment '{}' to {} student(s)", assessment.getTitle(), students.size());
        learningMailService.sendAssessmentPublished(students, assessment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLiveSessionScheduled(LiveSessionScheduledEvent event) {
        if (!properties.isLiveSessionEnabled()) return;

        LiveSession session = liveSessionRepository.findById(event.sessionId()).orElse(null);
        if (session == null) {
            log.warn("Skipping live class mail: session {} is gone", event.sessionId());
            return;
        }
        List<User> students = audience.enrolledStudents(event.courseId());
        if (students.isEmpty()) {
            log.info("Live class '{}' has no enrolled students to mail", session.getTitle());
            return;
        }

        log.info("Mailing live class '{}' to {} student(s)", session.getTitle(), students.size());
        learningMailService.sendLiveSessionScheduled(students, session, session.getCourse().getTitle());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCampusResourcePublished(CampusResourcePublishedEvent event) {
        if (!properties.isCampusToolkitEnabled()) return;

        CampusResource resource = campusResourceRepository.findById(event.resourceId()).orElse(null);
        if (resource == null) {
            log.warn("Skipping campus toolkit mail: resource {} is gone", event.resourceId());
            return;
        }
        List<User> students = audience.activeStudents();
        if (students.isEmpty()) return;

        log.info("Mailing toolkit resource '{}' to {} student(s)", resource.getTitle(), students.size());
        learningMailService.sendCampusResourcePublished(students, resource);
    }
}
