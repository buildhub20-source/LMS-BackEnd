package com.lms.platform.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreateAnnouncementRequest(
        @NotBlank String title,
        @NotBlank String message,
        String type,
        Boolean active,
        Instant startsAt,
        Instant expiresAt
) {}
