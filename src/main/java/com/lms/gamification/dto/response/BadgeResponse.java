package com.lms.gamification.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BadgeResponse(
        UUID id,
        String name,
        String description,
        String icon,
        String category,
        String criteriaType,
        Integer criteriaValue,
        boolean active,
        Instant awardedAt
) {}
