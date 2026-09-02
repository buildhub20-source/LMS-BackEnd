package com.lms.common.dto.internal;

import java.util.UUID;

/**
 * Minimal user projection returned by the internal API for the cert service.
 * Never includes credential or sensitive fields.
 */
public record InternalUserDto(
        UUID id,
        String name,
        String email
) {}
