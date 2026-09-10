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
        String recordingPlaybackUrl,
        Integer recordingDurationSeconds,
        boolean showResultAnalytics,
        Double percentileRank,
        ResultAnalyticsSummaryDto classAnalytics,
        List<GradeDistributionDto> gradeDistribution,
        List<QuestionResultDto> questionResults,
        List<AttemptHistoryResponse> attemptHistory
) {
    public record ResultAnalyticsSummaryDto(
            double classAverageScore,
            int highestScore,
            int lowestScore,
            double passPercentage,
            long totalParticipants
    ) {}

    public record GradeDistributionDto(
            String gradeLetter,
            String rangeLabel,
            long count,
            double percentage
    ) {}

    public record TestCaseResultDto(
            UUID testCaseId,
            String inputData,
            String expectedOutput,
            String actualOutput,
            boolean passed,
            boolean sample,
            boolean hidden,
            int weight
    ) {}

    public record QuestionResultDto(
            UUID questionId,
            String questionTitle,
            String questionDescription,
            String questionType,
            int maxMarks,
            Integer scoreEarned,
            String submissionStatus,
            String sourceCode,
            String language,
            String executionOutput,
            String officialExplanation,
            List<RubricEvaluationDto> rubricEvaluations,
            List<QuestionOptionResponse> options,
            List<TestCaseResultDto> testCases
    ) {}

    public record RubricEvaluationDto(
            UUID criterionId,
            String criterionName,
            int score,
            int maxPoints,
            String feedback
    ) {}
}
