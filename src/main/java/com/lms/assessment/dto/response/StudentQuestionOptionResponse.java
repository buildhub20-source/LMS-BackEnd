package com.lms.assessment.dto.response;

import java.util.UUID;

/**
 * Student-facing option response during test taking.
 * Deliberately excludes {@code isCorrect} and {@code explanation} to prevent cheating.
 */
public record StudentQuestionOptionResponse(
        UUID id,
        String optionText,
        int orderIndex
) {}
