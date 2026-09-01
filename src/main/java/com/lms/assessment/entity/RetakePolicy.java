package com.lms.assessment.entity;

/**
 * Strategy for computing a student's official score when multiple assessment attempts are permitted.
 */
public enum RetakePolicy {
    BEST_SCORE,
    LATEST_SCORE,
    AVERAGE_SCORE
}
