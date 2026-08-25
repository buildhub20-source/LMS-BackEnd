package com.lms.assessment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload for adding or updating a test case.
 */
public record CreateTestCaseRequest(

        String inputData,

        @NotBlank(message = "Expected output is required")
        String expectedOutput,

        @NotNull
        Boolean sample,

        @NotNull
        Boolean hidden,

        @Positive(message = "Weight must be positive")
        Integer weight
) {
    public CreateTestCaseRequest withDefaults() {
        return new CreateTestCaseRequest(
                inputData,
                expectedOutput,
                sample != null ? sample : false,
                hidden != null ? hidden : true,
                weight != null ? weight : 1
        );
    }
}
