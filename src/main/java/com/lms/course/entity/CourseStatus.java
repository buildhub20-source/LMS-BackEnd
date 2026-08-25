package com.lms.course.entity;

/**
 * Represents the lifecycle state of a {@link Course}.
 *
 * <p>Valid transitions:
 * <pre>
 *   DRAFT          → PENDING_REVIEW  (instructor submit)
 *   DRAFT          → PUBLISHED       (admin direct-publish)
 *   PENDING_REVIEW → PUBLISHED       (admin approve)
 *   PENDING_REVIEW → DRAFT           (admin reject)
 *   PUBLISHED      → UNPUBLISHED     (admin unpublish)
 *   UNPUBLISHED    → PUBLISHED       (admin republish)
 *   PUBLISHED      → ARCHIVED        (admin archive)
 *   UNPUBLISHED    → ARCHIVED        (admin archive)
 *   ARCHIVED       → (terminal)
 * </pre>
 */
public enum CourseStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    UNPUBLISHED,
    ARCHIVED
}
