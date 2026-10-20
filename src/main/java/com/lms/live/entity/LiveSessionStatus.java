package com.lms.live.entity;

public enum LiveSessionStatus {
    SCHEDULED,
    LIVE,
    ENDED,
    CANCELLED,
    /** Its time slot passed without the class being started. */
    MISSED
}
