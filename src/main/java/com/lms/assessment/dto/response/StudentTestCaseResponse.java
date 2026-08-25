package com.lms.assessment.dto.response;

import java.util.UUID;

/**
 * Student-safe test case response — includes ONLY sample test cases.
 * Hidden test cases and evaluation criteria are never exposed to students.
 */
public record StudentTestCaseResponse(
        UUID id,
        String inputData,
        String expectedOutput
) {}
