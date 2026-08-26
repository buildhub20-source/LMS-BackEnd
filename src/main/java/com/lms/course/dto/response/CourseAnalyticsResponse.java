package com.lms.course.dto.response;

import com.lms.assessment.dto.response.ScoreDistributionBucketDto;
import java.util.List;
import java.util.UUID;

public record CourseAnalyticsResponse(
    UUID courseId,
    String courseTitle,
    int totalLessonsCount,
    long totalEnrolledStudents,
    long attendedCount,
    long nonAttendedCount,
    long completedCount,
    long inProgressCount,
    double averageCompletionPercentage,
    List<StudentCourseStatDto> studentStats,
    List<ScoreDistributionBucketDto> progressDistribution
) {}
