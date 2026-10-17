package com.lms.assessment.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * An AI grading suggestion. {@code result} holds the per-test results, rubric
 * scores with comments, overall feedback and suggestions; it is null when
 * {@code status} is FAILED.
 */
public record AiGradingSuggestionResponse(
        UUID id,
        UUID submissionId,
        String status,
        BigDecimal score,
        Integer maxScore,
        Integer testsPassed,
        Integer testsTotal,
        JsonNode result,
        String errorCode,
        String errorMessage,
        Instant createdAt
) {}
