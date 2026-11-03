package com.lms.live.event;

import java.util.UUID;

/** Raised when a live session is placed on the calendar for a course. */
public record LiveSessionScheduledEvent(UUID sessionId, UUID courseId) {
}
