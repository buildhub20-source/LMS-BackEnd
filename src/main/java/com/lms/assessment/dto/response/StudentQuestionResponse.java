package com.lms.assessment.dto.response;

import com.lms.assessment.entity.Difficulty;

import java.util.List;
import java.util.UUID;

/**
 * Student-facing question view — includes only sample test cases.
 */
public record StudentQuestionResponse(
        UUID id,
        String title,
        String description,
        String inputFormat,
        String outputFormat,
        String constraints,
        Difficulty difficulty,
        int marks,
        int timeLimitMs,
        int memoryLimitMb,
        int questionOrder,
        UUID sectionId, // Added for UI grouping
        List<StudentTestCaseResponse> sampleTestCases
) {}
