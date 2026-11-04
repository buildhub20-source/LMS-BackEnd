package com.lms.common.sharedstate;

import java.time.Duration;

/**
 * Counts requests in fixed windows. With Redis enabled the counts are shared by
 * every copy of the backend, so a limit means the same thing however many copies
 * run; without it each copy counts on its own (fine for a single instance).
 */
public interface RateLimitStore {

    /**
     * Adds {@code delta} (may be negative, for refunds) to the count for {@code key}.
     * The window starts on the first hit and lasts {@code window}.
     */
    Hit add(String key, long delta, Duration window);

    /** Count after this hit, and milliseconds until the window resets. */
    record Hit(long count, long resetsInMillis) {}
}
