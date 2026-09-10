package com.lms.assessment.dto.response;

import java.util.UUID;

public record QuestionOptionResponse(
        UUID id,
        String optionText,
        boolean isCorrect,
        int orderIndex,
        String explanation
) {}
