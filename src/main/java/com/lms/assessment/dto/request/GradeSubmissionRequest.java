package com.lms.assessment.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record GradeSubmissionRequest(
        @NotNull(message = "Submission ID is required")
        UUID submissionId,

        Integer manualScore,

        String status,

        String instructorFeedback,

        List<RubricScoreRequest> rubricScores
) {}
