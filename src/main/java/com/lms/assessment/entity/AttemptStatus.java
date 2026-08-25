package com.lms.assessment.entity;

/** Lifecycle state of a student's {@link AssessmentAttempt}. */
public enum AttemptStatus {

    /** Created but the student has not yet opened the assessment. */
    NOT_STARTED,

    /** Student has opened the assessment and the timer is running. */
    IN_PROGRESS,

    /** Student explicitly clicked "Submit". Terminal state. */
    SUBMITTED,

    /** Server-side expiry check determined the timer elapsed. Terminal state. */
    EXPIRED
}
