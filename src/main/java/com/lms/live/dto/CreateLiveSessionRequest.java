package com.lms.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateLiveSessionRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull Instant scheduledStart,
        @NotNull Instant scheduledEnd
) {}
