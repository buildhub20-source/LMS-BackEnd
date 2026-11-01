package com.lms.live.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The reminder windows, stated as the sweep applies them: a session is due for
 * a reminder when its start falls in {@code (now + minimumLead, now + leadTime]}.
 */
@DisplayName("LiveSessionReminderKind windows")
class LiveSessionReminderKindTest {

    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

    private static boolean isDue(LiveSessionReminderKind kind, Duration startsIn) {
        Instant start = NOW.plus(startsIn);
        return start.isAfter(NOW.plus(kind.minimumLead()))
                && !start.isAfter(NOW.plus(kind.leadTime()));
    }

    @Test
    @DisplayName("the day-before reminder covers a class between two and twenty-four hours out")
    void dayBeforeCoversItsWindow() {
        assertThat(isDue(LiveSessionReminderKind.DAY_BEFORE, Duration.ofHours(23))).isTrue();
        assertThat(isDue(LiveSessionReminderKind.DAY_BEFORE, Duration.ofHours(5))).isTrue();
        assertThat(isDue(LiveSessionReminderKind.DAY_BEFORE, Duration.ofHours(24))).isTrue();
    }

    @Test
    @DisplayName("a class more than a day out is not yet due")
    void dayBeforeNotDueTooEarly() {
        assertThat(isDue(LiveSessionReminderKind.DAY_BEFORE, Duration.ofHours(25))).isFalse();
        assertThat(isDue(LiveSessionReminderKind.DAY_BEFORE, Duration.ofDays(3))).isFalse();
    }

    @Test
    @DisplayName("a class booked at short notice never gets the day-before reminder")
    void dayBeforeSuppressedInsideTheFloor() {
        // This is the case that would otherwise arrive moments before the
        // 15-minute reminder and read as duplicate mail.
        assertThat(isDue(LiveSessionReminderKind.DAY_BEFORE, Duration.ofMinutes(20))).isFalse();
        assertThat(isDue(LiveSessionReminderKind.DAY_BEFORE, Duration.ofMinutes(90))).isFalse();
        assertThat(isDue(LiveSessionReminderKind.DAY_BEFORE, Duration.ofHours(2))).isFalse();
    }

    @Test
    @DisplayName("the imminent reminder covers only the last quarter hour")
    void fifteenMinuteWindow() {
        assertThat(isDue(LiveSessionReminderKind.FIFTEEN_MINUTES, Duration.ofMinutes(15))).isTrue();
        assertThat(isDue(LiveSessionReminderKind.FIFTEEN_MINUTES, Duration.ofMinutes(5))).isTrue();
        assertThat(isDue(LiveSessionReminderKind.FIFTEEN_MINUTES, Duration.ofMinutes(16))).isFalse();
    }

    @Test
    @DisplayName("a class already under way is due for neither")
    void nothingDueOnceStarted() {
        for (LiveSessionReminderKind kind : LiveSessionReminderKind.values()) {
            assertThat(isDue(kind, Duration.ZERO)).as("%s at start", kind).isFalse();
            assertThat(isDue(kind, Duration.ofMinutes(-5))).as("%s after start", kind).isFalse();
        }
    }

    @Test
    @DisplayName("the two windows never overlap, so no class gets both at once")
    void windowsDoNotOverlap() {
        for (long minutes = 1; minutes <= 24 * 60; minutes++) {
            Duration in = Duration.ofMinutes(minutes);
            boolean both = isDue(LiveSessionReminderKind.DAY_BEFORE, in)
                    && isDue(LiveSessionReminderKind.FIFTEEN_MINUTES, in);
            assertThat(both).as("both reminders due for a class %d minutes out", minutes).isFalse();
        }
    }
}
