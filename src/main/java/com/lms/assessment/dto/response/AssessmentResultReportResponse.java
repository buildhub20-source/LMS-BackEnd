package com.lms.assessment.dto.response;

import com.lms.assessment.entity.AttemptStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssessmentResultReportResponse(
        UUID attemptId,
        UUID assessmentId,
        String assessmentTitle,
        UUID studentId,
        String studentName,
        AttemptStatus status,
        Integer finalScore,
        int totalMarks,
        double percentage,
        boolean passed,
        String retakePolicy,
        int attemptsUsed,
        int maxAttemptsAllowed,
        long timeSpentSeconds,
        Instant startedAt,
        Instant submittedAt,
        List<QuestionResultDto> questionResults,
        List<AttemptHistoryResponse> attemptHistory
) {
    public record QuestionResultDto(
            UUID questionId,
            String questionTitle,
            String questionType,
            int maxMarks,
            Integer scoreEarned,
            String submissionStatus,
            String sourceCode,
            List<RubricEvaluationDto> rubricEvaluations
    ) {}

    public record RubricEvaluationDto(
            UUID criterionId,
            String criterionName,
            int score,
            int maxPoints,
            String feedback
    ) {}
}
