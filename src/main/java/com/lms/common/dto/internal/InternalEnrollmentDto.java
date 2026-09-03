package com.lms.common.dto.internal;

import java.time.Instant;
import java.util.UUID;

/**
 * Minimal enrollment projection returned by the internal API for the cert service.
 */
public record InternalEnrollmentDto(
        UUID id,
        UUID studentId,
        UUID courseId,
        String status,
        Instant enrolledAt,
        Instant completedAt
) {}
