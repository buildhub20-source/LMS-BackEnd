package com.lms.assessment.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RubricResponse(
        UUID id,
        String title,
        String description,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<RubricCriterionResponse> criteria
) {
    public record RubricCriterionResponse(
            UUID id,
            String criterionName,
            String description,
            int maxPoints,
            double weight
    ) {}
}
