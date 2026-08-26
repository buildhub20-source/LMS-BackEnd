package com.lms.assessment.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a section, including its nested questions.
 */
public record SectionResponse(
        UUID id,
        String title,
        String description,
        int sectionOrder,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionResponse> questions
) {}
