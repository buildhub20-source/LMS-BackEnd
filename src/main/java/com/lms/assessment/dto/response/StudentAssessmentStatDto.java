package com.lms.assessment.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StudentAssessmentStatDto(
        UUID studentId,
        String studentName,
        String studentEmail,
        String status, // "COMPLETED", "IN_PROGRESS", "NOT_ATTENDED", "EXPIRED"
        Integer score,
        Integer totalMarks,
        Double completionPercentage,
        String gradeLetter,
        Boolean passed,
        Integer attemptsCount,
        Instant submittedAt
) {}
