package com.lms.platform.runtime;

import java.util.UUID;

/** Short-lived, request-scoped connection metadata; never returned to clients. */
public record TenantConnection(UUID tenantId, String slug, String jdbcUrl, String username, String password) {}
