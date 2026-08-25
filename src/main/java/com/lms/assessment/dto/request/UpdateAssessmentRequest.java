package com.lms.assessment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Payload for updating an existing DRAFT assessment.
 *
 * <p>All fields are optional; null values are ignored (partial update semantics).
 * {@code totalMarks} is intentionally absent: it is auto-computed from the sum
 * of all question marks and must not be set manually.
 * An assessment can only be updated while it is in DRAFT status.
 */
public record UpdateAssessmentRequest(

        @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
        String title,

        String description,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Max(value = 1440, message = "Duration cannot exceed 24 hours (1440 minutes)")
        Integer durationMinutes,

        @Min(value = 1, message = "Max attempts must be at least 1")
        @Max(value = 10, message = "Max attempts cannot exceed 10")
        Integer maxAttempts,

        Instant startTime,

        Instant endTime
) {}
