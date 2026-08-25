package com.lms.assessment.dto.request;

import com.lms.assessment.entity.Difficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for updating an existing question.
 */
public record UpdateQuestionRequest(

        @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
        String title,

        String description,

        String inputFormat,

        String outputFormat,

        String constraints,

        Difficulty difficulty,

        String compiler,

        @Min(value = 1, message = "Marks must be at least 1")
        @Max(value = 100, message = "Marks cannot exceed 100")
        Integer marks,

        @Min(value = 100, message = "Time limit must be at least 100 ms")
        @Max(value = 10000, message = "Time limit cannot exceed 10000 ms")
        Integer timeLimitMs,

        @Min(value = 16, message = "Memory limit must be at least 16 MB")
        @Max(value = 1024, message = "Memory limit cannot exceed 1024 MB")
        Integer memoryLimitMb,

        @Valid
        List<CreateTestCaseRequest> testCases
) {}
