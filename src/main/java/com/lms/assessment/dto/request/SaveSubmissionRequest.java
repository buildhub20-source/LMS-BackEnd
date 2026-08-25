package com.lms.assessment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Payload for student code draft autosave.
 */
public record SaveSubmissionRequest(

        @NotNull(message = "Question ID is required")
        UUID questionId,

        @NotBlank(message = "Language is required")
        String language,

        String sourceCode
) {}
