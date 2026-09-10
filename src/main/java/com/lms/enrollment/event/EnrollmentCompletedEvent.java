package com.lms.enrollment.event;

import java.util.UUID;

/** Immutable completion payload, captured while the tenant context is active. */
public record EnrollmentCompletedEvent(UUID studentId, UUID courseId, String tenantSlug) {
}
