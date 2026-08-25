package com.lms.assessment.entity;

/** Lifecycle state of an {@link Assessment}. */
public enum AssessmentStatus {

    /** Visible only to admins; not accessible by students. */
    DRAFT,

    /** Live and accessible by students (subject to start/end time window). */
    PUBLISHED,

    /** No longer accepting new attempts but existing attempts still visible. */
    CLOSED,

    /** Soft-deleted / moved to archive; hidden from all listing UIs. */
    ARCHIVED
}
