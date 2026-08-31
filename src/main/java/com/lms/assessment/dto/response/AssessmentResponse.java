package com.lms.assessment.dto.response;

import com.lms.assessment.entity.AssessmentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Full detail response for a single assessment.
 * Returned by GET /api/v1/admin/assessments/{id} and POST (create).
 */
public record AssessmentResponse(

        UUID id,
        String title,
        String description,
        int durationMinutes,
        int totalMarks,
        int maxAttempts,
        boolean randomizeQuestions,
        String retakePolicy,
        AssessmentStatus status,
        Instant startTime,
        Instant endTime,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,

        /** Number of questions currently attached to this assessment. */
        long questionCount
) {}
