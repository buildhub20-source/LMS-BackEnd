package com.lms.assessment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GenerateAttemptRecordingUploadUrlRequest(
        @NotBlank(message = "Content type is required")
        String contentType,
        String fileName,
        Integer durationSeconds
) {}
