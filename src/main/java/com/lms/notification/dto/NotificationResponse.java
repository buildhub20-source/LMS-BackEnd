package com.lms.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String message,
        String link,
        boolean read,
        Instant createdAt,
        Instant readAt
) {}
