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
        double averageScore,
        double averageCompletionPercentage,
        List<StudentAssessmentStatDto> studentStats,
        List<ScoreDistributionBucketDto> scoreDistribution
) {}
