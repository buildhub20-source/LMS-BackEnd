package com.lms.enrollment.dto.response;

import com.lms.enrollment.entity.EnrollmentStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record EnrollmentResponse(
        UUID id,
        StudentSummary student,
        CourseSummary course,
        EnrollmentStatus status,
        Instant enrolledAt,
        Instant startedAt,
        Instant completedAt,
        Instant lastAccessedAt
) {
    @Builder
    public record StudentSummary(
            UUID id,
            String email,
            String fullName
    ) {}

    @Builder
    public record CourseSummary(
            UUID id,
            String title,
            UUID instructorId
    ) {}
}
