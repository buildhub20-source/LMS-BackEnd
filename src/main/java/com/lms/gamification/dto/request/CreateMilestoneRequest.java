package com.lms.gamification.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMilestoneRequest(
        @NotBlank String key,
        @NotBlank String name,
        String description,
        @NotBlank String icon,
        @NotBlank String criteriaType,
        @NotNull @Min(1) Integer criteriaValue,
        Integer sortOrder,
        boolean active
) {}
