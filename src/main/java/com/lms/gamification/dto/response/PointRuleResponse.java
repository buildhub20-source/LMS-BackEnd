package com.lms.gamification.dto.response;

import java.util.UUID;

public record PointRuleResponse(
        UUID id,
        String eventType,
        int points,
        boolean active
) {}
