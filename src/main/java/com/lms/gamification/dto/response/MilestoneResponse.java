package com.lms.gamification.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MilestoneResponse(
        UUID id,
        String key,
        String name,
        String description,
        String icon,
        String criteriaType,
        int criteriaValue,
        int sortOrder,
        boolean active,
        boolean completed,
        Instant completedAt
) {}
