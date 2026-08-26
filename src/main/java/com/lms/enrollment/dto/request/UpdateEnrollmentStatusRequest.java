package com.lms.enrollment.dto.request;

import com.lms.enrollment.entity.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEnrollmentStatusRequest(
        @NotNull(message = "Status is required")
        EnrollmentStatus status
) {}
