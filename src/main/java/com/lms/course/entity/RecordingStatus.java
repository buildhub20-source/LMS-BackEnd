package com.lms.course.entity;

/** Status of a recording upload/processing pipeline. */
public enum RecordingStatus {
    /** Upload initiated but not yet confirmed. */
    PENDING,
    /** File uploaded and ready for playback. */
    READY,
    /** Upload or processing failed. */
    FAILED
}
