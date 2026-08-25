package com.lms.assessment.dto.response;

import java.util.UUID;

/**
 * Response payload for a test case.
 */
public record TestCaseResponse(
        UUID id,
        String inputData,
        String expectedOutput,
        boolean sample,
        boolean hidden,
        int weight
) {}
