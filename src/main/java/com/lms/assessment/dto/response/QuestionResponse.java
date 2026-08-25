package com.lms.assessment.dto.response;

import com.lms.assessment.entity.Difficulty;
import com.lms.assessment.entity.QuestionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin response payload for a question with all test cases.
 */
public record QuestionResponse(
        UUID id,
        String title,
        String description,
        String inputFormat,
        String outputFormat,
        String constraints,
        Difficulty difficulty,
        QuestionType questionType,
        int marks,
        int timeLimitMs,
        int memoryLimitMb,
        int questionOrder,
        Instant createdAt,
        Instant updatedAt,
        List<TestCaseResponse> testCases
) {}
