package com.lms.common.dto.internal;

import java.util.UUID;

/**
 * Tenant connection projection returned by the internal API for trusted
 * microservices (e.g. lms-chat-service) that need to connect to tenant
 * databases directly.
 *
 * <p>Contains decrypted database credentials and must never be exposed
 * to external clients.
 */
public record InternalTenantDto(
        UUID tenantId,
        String slug,
        String jdbcUrl,
        String username,
        String password
) {}
