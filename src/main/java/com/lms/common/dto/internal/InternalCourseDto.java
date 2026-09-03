package com.lms.common.dto.internal;

import java.util.UUID;

/**
 * Minimal course projection returned by the internal API for the cert service.
 */
public record InternalCourseDto(
        UUID id,
        String title,
        String description,
        Integer durationMinutes,
        String thumbnailKey
) {}
