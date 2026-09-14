package com.lms.assessment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExtendAssessmentRequest(
        @NotNull(message = "Minutes cannot be null")
        @Min(value = 1, message = "Minutes must be at least 1")
        Integer minutes
) {}
