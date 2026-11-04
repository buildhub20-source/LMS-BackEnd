package com.lms.common.sharedstate;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Per-process counts. Used when Redis is not configured, and in tests. */
public class InMemoryRateLimitStore implements RateLimitStore {

    private static final int PRUNE_THRESHOLD = 10_000;

    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimitStore() {
        this(Clock.systemUTC());
    }

    public InMemoryRateLimitStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Hit add(String key, long delta, Duration window) {
        long now = clock.millis();
        long windowMillis = window.toMillis();
        if (windows.size() > PRUNE_THRESHOLD) {
            windows.values().removeIf(w -> now - w.start >= w.length);
        }
        Window updated = windows.compute(key, (k, current) ->
                current == null || now - current.start >= current.length
                        ? new Window(now, windowMillis, delta)
                        : new Window(current.start, current.length, current.count + delta));
        return new Hit(updated.count, Math.max(0, updated.start + updated.length - now));
    }

    private record Window(long start, long length, long count) {}
}
