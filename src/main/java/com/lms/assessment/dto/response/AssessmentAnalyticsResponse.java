package com.lms.assessment.dto.response;

import java.util.List;
import java.util.UUID;

public record AssessmentAnalyticsResponse(
        UUID assessmentId,
        String assessmentTitle,
        int totalMarks,
        long totalEnrolledStudents,
        long attendedCount,
        long nonAttendedCount,
        long completedCount,
        long inProgressCount,
        long pendingGradingCount,
        long passedCount,
        long failedCount,
        double passPercentage,
        double averageScore,
        double averageCompletionPercentage,
        int highestScore,
        int lowestScore,
        List<StudentAssessmentStatDto> studentStats,
        List<ScoreDistributionBucketDto> scoreDistribution,
        List<GradeDistributionDto> gradeDistribution
) {}
