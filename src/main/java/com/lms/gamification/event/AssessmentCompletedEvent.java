package com.lms.gamification.event;

import java.util.UUID;

/** Published when a student submits an assessment attempt. */
public record AssessmentCompletedEvent(
        UUID studentId,
        UUID assessmentId,
        UUID attemptId,
        Integer score,
        boolean passed,
        Integer maxScore
) {}
