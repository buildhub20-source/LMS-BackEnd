package com.lms.assessment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Payload for creating a new assessment (initially saved as DRAFT).
 */
public record CreateAssessmentRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be 255 characters or fewer")
        String title,

        String description,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Max(value = 1440, message = "Duration cannot exceed 24 hours (1440 minutes)")
        Integer durationMinutes,

        @Min(value = 0, message = "Total marks cannot be negative")
        Integer totalMarks,

        @Min(value = 1, message = "Max attempts must be at least 1")
        @Max(value = 10, message = "Max attempts cannot exceed 10")
        Integer maxAttempts,

        Boolean randomizeQuestions,

        String retakePolicy,

        /** Optional: if null, the assessment window is open-ended once published. */
        Instant startTime,

        Instant endTime
) {
    /**
     * Apply defaults for optional fields not provided by the caller.
     */
    public CreateAssessmentRequest withDefaults() {
        return new CreateAssessmentRequest(
                title,
                description,
                durationMinutes != null ? durationMinutes : 60,
                totalMarks != null ? totalMarks : 0,
                maxAttempts != null ? maxAttempts : 1,
                randomizeQuestions != null ? randomizeQuestions : false,
                retakePolicy != null ? retakePolicy : "BEST_SCORE",
                startTime,
                endTime
        );
    }
}
