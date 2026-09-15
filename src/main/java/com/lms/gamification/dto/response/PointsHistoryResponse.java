package com.lms.gamification.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PointsHistoryResponse(
        UUID id,
        int points,
        String eventType,
        UUID sourceEntityId,
        Instant createdAt
) {}
