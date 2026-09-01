package com.lms.assessment.dto.response;

import java.util.UUID;

public record GenerateAttemptRecordingUploadUrlResponse(
        UUID attemptId,
        String uploadUrl,
        String key,
        String publicUrl,
        boolean directUploadAvailable
) {}
