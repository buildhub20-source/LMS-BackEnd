package com.lms.assessment.dto.response;

import com.lms.assessment.entity.AttemptStatus;

import java.time.Instant;
import java.util.UUID;

public record AttemptHistoryResponse(
        UUID attemptId,
        UUID assessmentId,
        String assessmentTitle,
        int attemptNumber,
        AttemptStatus status,
        Integer score,
        int totalMarks,
        double percentage,
        Instant startedAt,
        Instant submittedAt,
        Instant expiresAt
) {}
