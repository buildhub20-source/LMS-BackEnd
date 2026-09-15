package com.lms.gamification.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBadgeRequest(
        @NotBlank String name,
        String description,
        @NotBlank String icon,
        String category,
        @NotBlank String criteriaType,
        @NotNull @Min(1) Integer criteriaValue,
        boolean active
) {}
