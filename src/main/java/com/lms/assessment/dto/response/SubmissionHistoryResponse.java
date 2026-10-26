package com.lms.assessment.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SubmissionHistoryResponse(
        UUID assessmentId, String assessmentTitle, int assessmentTotalMarks,
        int assessmentDuration, String assessmentStatus,
        UUID studentId, String studentName, String studentEmail,
        String status, Integer score, int totalMarks, Double completionPercentage,
        String gradeLetter, Boolean passed, int attemptsCount, Instant submittedAt) {}
