package com.lms.platform.dto;

import com.lms.platform.entity.BroadcastAnnouncement;

import java.time.Instant;
import java.util.UUID;

public record BroadcastAnnouncementDto(
        UUID id,
        String title,
        String message,
        String type,
        boolean active,
        Instant startsAt,
        Instant expiresAt,
        UUID createdBy,
        Instant createdAt
) {
    public static BroadcastAnnouncementDto from(BroadcastAnnouncement b) {
        return new BroadcastAnnouncementDto(
                b.getId(),
                b.getTitle(),
                b.getMessage(),
                b.getType(),
                b.isActive(),
                b.getStartsAt(),
                b.getExpiresAt(),
                b.getCreatedBy(),
                b.getCreatedAt()
        );
    }
}
