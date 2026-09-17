package com.lms.gamification.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateBadgeRequest(
        @NotBlank String name,
        String description,
        @NotBlank String icon,
        String category,
        @NotBlank @Pattern(regexp = "LESSONS_COMPLETED|COURSES_COMPLETED|ASSESSMENTS_PASSED|HIGH_SCORE|STREAK_DAYS") String criteriaType,
        @NotNull @Min(1) Integer criteriaValue,
        boolean active
) {}
