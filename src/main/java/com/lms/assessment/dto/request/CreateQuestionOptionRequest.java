package com.lms.assessment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateQuestionOptionRequest(
        UUID id,

        @NotBlank(message = "Option text is required")
        String optionText,

        boolean isCorrect,

        Integer orderIndex,

        String explanation
) {}
