package com.lms.enrollment.event;

import java.util.UUID;

/**
 * Raised when a student is enrolled in a course.
 *
 * <p>This, not course publication, is what tells a student a course is theirs:
 * a freshly published course has no students attached to it yet.
 */
public record EnrollmentCreatedEvent(UUID studentId, UUID courseId) {
}
