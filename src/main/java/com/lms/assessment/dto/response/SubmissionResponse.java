package com.lms.assessment.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Student submission response DTO.
 */
public record SubmissionResponse(
        UUID id,
        UUID attemptId,
        UUID questionId,
        String language,
        String sourceCode,
        String status,
        Instant submittedAt
) {}
