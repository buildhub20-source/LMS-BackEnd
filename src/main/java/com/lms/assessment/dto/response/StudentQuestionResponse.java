package com.lms.assessment.dto.response;

import com.lms.assessment.entity.Difficulty;
import com.lms.assessment.entity.QuestionType;

import java.util.List;
import java.util.UUID;

/**
 * Student-facing question view — includes only sample test cases for coding
 * questions, and safe options (without isCorrect) for multiple choice questions.
 */
public record StudentQuestionResponse(
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
        List<StudentTestCaseResponse> sampleTestCases,
        List<StudentQuestionOptionResponse> options
) {}
