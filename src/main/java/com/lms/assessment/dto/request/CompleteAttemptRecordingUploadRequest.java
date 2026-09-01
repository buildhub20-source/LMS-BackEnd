package com.lms.assessment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CompleteAttemptRecordingUploadRequest(
        @NotBlank(message = "Recording key is required")
        String key,
        Integer durationSeconds
) {}
