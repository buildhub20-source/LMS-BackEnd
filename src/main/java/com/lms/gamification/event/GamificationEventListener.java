package com.lms.gamification.event;

import com.lms.enrollment.event.EnrollmentCompletedEvent;
import com.lms.gamification.entity.GamificationEventType;
import com.lms.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens to domain events from course, enrollment, and assessment modules
 * and triggers gamification updates.
 *
 * <p>Runs after the source transaction commits to avoid awarding points
 * for events that might be rolled back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GamificationEventListener {

    private final GamificationService gamificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLessonCompleted(LessonCompletedEvent event) {
        try {
            log.debug("Gamification: lesson completed — student={}, lesson={}", event.studentId(), event.lessonId());
            gamificationService.awardPoints(event.studentId(), GamificationEventType.LESSON_COMPLETION, event.lessonId());
            gamificationService.recordDailyActivity(event.studentId());
            gamificationService.checkAndAwardBadges(event.studentId());
            gamificationService.checkAndAwardMilestones(event.studentId());
        } catch (Exception ex) {
            log.warn("Gamification processing failed for lesson event: {}", ex.getMessage(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnrollmentCompleted(EnrollmentCompletedEvent event) {
        try {
            log.debug("Gamification: course completed — student={}, course={}", event.studentId(), event.courseId());
            gamificationService.awardPoints(event.studentId(), GamificationEventType.COURSE_COMPLETION, event.courseId());
            gamificationService.recordDailyActivity(event.studentId());
            gamificationService.checkAndAwardBadges(event.studentId());
            gamificationService.checkAndAwardMilestones(event.studentId());
        } catch (Exception ex) {
            log.warn("Gamification processing failed for enrollment event: {}", ex.getMessage(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssessmentCompleted(AssessmentCompletedEvent event) {
        try {
            log.debug("Gamification: assessment completed — student={}, assessment={}", event.studentId(), event.assessmentId());
            gamificationService.awardPoints(event.studentId(), GamificationEventType.ASSESSMENT_COMPLETION, event.attemptId());

            if (event.passed()) {
                gamificationService.awardPoints(event.studentId(), GamificationEventType.ASSESSMENT_PASS, event.attemptId());
            }

            // High score: >= 90%
            if (event.score() != null && event.maxScore() != null && event.maxScore() > 0) {
                double pct = 100.0 * event.score() / event.maxScore();
                if (pct >= 90.0) {
                    gamificationService.awardPoints(event.studentId(), GamificationEventType.HIGH_SCORE, event.attemptId());
                }
            }

            gamificationService.recordDailyActivity(event.studentId());
            gamificationService.checkAndAwardBadges(event.studentId());
            gamificationService.checkAndAwardMilestones(event.studentId());
        } catch (Exception ex) {
            log.warn("Gamification processing failed for assessment event: {}", ex.getMessage(), ex);
        }
    }
}
