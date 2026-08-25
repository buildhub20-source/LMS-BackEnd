package com.lms.assessment.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request body for updating an existing section.
 */
public record UpdateSectionRequest(

        @Size(max = 255, message = "Section title must not exceed 255 characters")
        String title,

        String description
) {}
