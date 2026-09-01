package com.lms.assessment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateRubricRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotEmpty(message = "At least one criterion is required")
        List<RubricCriterionDto> criteria
) {}
