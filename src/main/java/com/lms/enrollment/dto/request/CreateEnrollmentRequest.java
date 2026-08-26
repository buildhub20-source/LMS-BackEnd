package com.lms.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateEnrollmentRequest(
        @NotNull(message = "Student ID is required")
        UUID studentId,

        @NotNull(message = "Course ID is required")
        UUID courseId
) {}
