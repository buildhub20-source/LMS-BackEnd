package com.lms.course.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StudentCourseStatDto(
    UUID studentId,
    String studentName,
    String studentEmail,
    String status,
    int lessonsCompleted,
    int totalLessons,
    double completionPercentage,
    Instant lastActivityAt
) {}
