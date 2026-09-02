package com.lms.platform.dto;

import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantStatus;

import java.time.Instant;
import java.util.UUID;

/** Deliberately excludes database credentials and owner password material. */
public record TenantResponse(
        UUID id, String name, String slug, TenantStatus status, String provider,
        String providerProjectRef, String ownerName, String ownerEmail,
        String failureReason, Instant provisionedAt, Instant suspendedAt,
        Instant deletionScheduledAt, Instant createdAt
) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.getStatus(),
                tenant.getProvider(), tenant.getProviderProjectRef(), tenant.getOwnerName(), tenant.getOwnerEmail(),
                tenant.getFailureReason(), tenant.getProvisionedAt(), tenant.getSuspendedAt(),
                tenant.getDeletionScheduledAt(), tenant.getCreatedAt());
    }
}
