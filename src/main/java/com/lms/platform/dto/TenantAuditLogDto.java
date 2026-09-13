package com.lms.platform.dto;

import com.lms.platform.entity.TenantAuditEvent;

import java.time.Instant;
import java.util.UUID;

public record TenantAuditLogDto(
        UUID id,
        UUID tenantId,
        String tenantSlug,
        String tenantName,
        UUID actorId,
        String eventType,
        String message,
        Instant createdAt
) {
    public static TenantAuditLogDto from(TenantAuditEvent event, String tenantSlug, String tenantName) {
        return new TenantAuditLogDto(
                event.getId(),
                event.getTenantId(),
                tenantSlug,
                tenantName,
                event.getActorId(),
                event.getEventType(),
                event.getMessage(),
                event.getCreatedAt()
        );
    }
}
