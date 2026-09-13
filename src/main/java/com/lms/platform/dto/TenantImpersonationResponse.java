package com.lms.platform.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantImpersonationResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        String tenantSlug,
        UUID tenantId,
        String targetUserEmail,
        String targetUserName,
        String targetRole,
        String workspaceUrl,
        String reason
) {}
