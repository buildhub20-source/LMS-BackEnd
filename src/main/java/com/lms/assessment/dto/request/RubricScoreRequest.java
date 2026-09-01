package com.lms.assessment.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RubricScoreRequest(
        @NotNull(message = "Criterion ID is required")
        UUID criterionId,

        @NotNull(message = "Score is required")
        Integer score,

        String feedback
) {}
