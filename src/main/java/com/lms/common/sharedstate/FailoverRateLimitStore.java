package com.lms.common.sharedstate;

import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;

/**
 * Uses the shared (Redis) store while it answers and falls back to per-process
 * counting when it does not. Without this, a Redis outage would make every
 * sign-in fail: the limit check runs before the login itself.
 *
 * <p>After a failure the shared store is left alone for {@link #RETRY_AFTER}, so
 * each request does not pay the connection timeout while Redis is down. During
 * that time each copy counts on its own, which is the single-copy behaviour:
 * limits are looser by the number of copies, never stricter.
 */
@Slf4j
public class FailoverRateLimitStore implements RateLimitStore {

    static final Duration RETRY_AFTER = Duration.ofSeconds(30);

    private final RateLimitStore shared;
    private final RateLimitStore local;
    private final Clock clock;
    private volatile long sharedUnavailableUntil;

    public FailoverRateLimitStore(RateLimitStore shared, RateLimitStore local) {
        this(shared, local, Clock.systemUTC());
    }

    FailoverRateLimitStore(RateLimitStore shared, RateLimitStore local, Clock clock) {
        this.shared = shared;
        this.local = local;
        this.clock = clock;
    }

    @Override
    public Hit add(String key, long delta, Duration window) {
        long now = clock.millis();
        if (now >= sharedUnavailableUntil) {
            try {
                return shared.add(key, delta, window);
            } catch (RuntimeException ex) {
                sharedUnavailableUntil = now + RETRY_AFTER.toMillis();
                log.warn("Shared rate-limit store unavailable; counting in this process for {}s: {}",
                        RETRY_AFTER.toSeconds(), ex.getMessage());
            }
        }
        return local.add(key, delta, window);
    }
}
