package com.lms.assessment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new section within an assessment.
 */
public record CreateSectionRequest(

        @NotBlank(message = "Section title is required")
        @Size(max = 255, message = "Section title must not exceed 255 characters")
        String title,

        String description
) {}
