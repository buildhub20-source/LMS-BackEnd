package com.lms.assessment.dto.response;

import com.lms.assessment.entity.AttemptStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload returned when a student starts an assessment attempt.
 * Contains server-authoritative timer details and question list.
 */
public record StartAttemptResponse(
        UUID attemptId,
        UUID assessmentId,
        String assessmentTitle,
        int durationMinutes,
        AttemptStatus status,
        Instant startedAt,
        Instant expiresAt,
        long remainingSeconds,
        List<StudentQuestionResponse> questions
) {}
