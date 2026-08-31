package com.lms.assessment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record RubricCriterionDto(
        UUID id,

        @NotBlank(message = "Criterion name is required")
        String criterionName,

        String description,

        @Min(value = 1, message = "Max points must be at least 1")
        @Max(value = 100, message = "Max points cannot exceed 100")
        int maxPoints,

        double weight
) {}
