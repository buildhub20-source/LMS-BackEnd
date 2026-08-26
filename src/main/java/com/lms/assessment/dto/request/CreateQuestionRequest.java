package com.lms.assessment.dto.request;

import com.lms.assessment.entity.Difficulty;
import com.lms.assessment.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for creating a new Question and associating it with an Assessment.
 */
public record CreateQuestionRequest(

        @NotBlank(message = "Question title is required")
        @Size(max = 500, message = "Title must be 500 characters or fewer")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        String inputFormat,

        String outputFormat,

        String constraints,

        @NotNull(message = "Difficulty is required")
        Difficulty difficulty,

        QuestionType questionType,

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
) {
    public CreateQuestionRequest withDefaults() {
        return new CreateQuestionRequest(
                title,
                description,
                inputFormat,
                outputFormat,
                constraints,
                difficulty != null ? difficulty : Difficulty.MEDIUM,
                questionType != null ? questionType : QuestionType.CODING,
                compiler != null && !compiler.isBlank() ? compiler : "ALL",
                marks != null ? marks : 10,
                timeLimitMs != null ? timeLimitMs : 2000,
                memoryLimitMb != null ? memoryLimitMb : 256,
                testCases != null ? testCases : List.of()
        );
    }
}
