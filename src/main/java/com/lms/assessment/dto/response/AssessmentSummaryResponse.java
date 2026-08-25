package com.lms.assessment.dto.response;

import com.lms.assessment.entity.AssessmentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight summary used in paginated list responses.
 * Does not include description or question details.
 */
public record AssessmentSummaryResponse(

        UUID id,
        String title,
        int durationMinutes,
        int totalMarks,
        int maxAttempts,
        AssessmentStatus status,
        Instant startTime,
        Instant endTime,
        Instant createdAt,
        long questionCount
) {}
