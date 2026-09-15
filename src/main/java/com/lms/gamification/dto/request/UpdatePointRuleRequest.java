package com.lms.gamification.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdatePointRuleRequest(
        @NotNull @Min(0) Integer points,
        boolean active
) {}
