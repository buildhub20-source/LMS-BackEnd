package com.lms.live.entity;

import java.time.Duration;

/** The two reminder mails sent ahead of a live class. */
public enum LiveSessionReminderKind {

    /**
     * Sent once the session is a day out, but never inside the last two hours.
     * A class booked at short notice would otherwise get this and the
     * 15-minute reminder almost at once, which reads as duplicate mail.
     */
    DAY_BEFORE(Duration.ofHours(24), Duration.ofHours(2)),

    /** Sent in the last quarter hour before the session starts. */
    FIFTEEN_MINUTES(Duration.ofMinutes(15), Duration.ZERO);

    private final Duration leadTime;
    private final Duration minimumLead;

    LiveSessionReminderKind(Duration leadTime, Duration minimumLead) {
        this.leadTime = leadTime;
        this.minimumLead = minimumLead;
    }

    /** How far ahead of {@code scheduledStart} this reminder becomes due. */
    public Duration leadTime() {
        return leadTime;
    }

    /** How close to the start this reminder stops being worth sending. */
    public Duration minimumLead() {
        return minimumLead;
    }
}
