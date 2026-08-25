package com.lms.assessment.dto.response;

import com.lms.assessment.entity.AttemptStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed attempt view with questions and student's current code submissions.
 */
public record AttemptDetailResponse(
        UUID attemptId,
        UUID assessmentId,
        String assessmentTitle,
        int durationMinutes,
        AttemptStatus status,
        Integer score,
        Instant startedAt,
        Instant expiresAt,
        Instant submittedAt,
        long remainingSeconds,
        List<StudentQuestionResponse> questions,
        List<SubmissionResponse> submissions
) {}
