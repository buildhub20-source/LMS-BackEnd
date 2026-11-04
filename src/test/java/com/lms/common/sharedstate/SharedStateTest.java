package com.lms.common.sharedstate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("in-process rate-limit store and cluster lock")
class SharedStateTest {

    private static final class MutableClock extends Clock {
        long millis;
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public long millis() { return millis; }
    }

    @Test
    @DisplayName("counts within a window, supports refunds, and resets after it")
    void windowCounting() {
        MutableClock clock = new MutableClock();
        InMemoryRateLimitStore store = new InMemoryRateLimitStore(clock);
        Duration window = Duration.ofSeconds(60);

        assertThat(store.add("k", 1, window).count()).isEqualTo(1);
        clock.millis = 10_000;
        RateLimitStore.Hit hit = store.add("k", 2, window);
        assertThat(hit.count()).isEqualTo(3);
        assertThat(hit.resetsInMillis()).isEqualTo(50_000);
        assertThat(store.add("k", -2, window).count()).isEqualTo(1);
        assertThat(store.add("other", 1, window).count()).isEqualTo(1);

        clock.millis = 60_000;
        assertThat(store.add("k", 1, window).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("a held lock makes an overlapping run skip, and is released afterwards")
    void localLock() {
        LocalClusterLock lock = new LocalClusterLock();
        AtomicBoolean innerRan = new AtomicBoolean();

        boolean outer = lock.runExclusively("job", Duration.ofMinutes(1),
                () -> innerRan.set(lock.runExclusively("job", Duration.ofMinutes(1), () -> {})));

        assertThat(outer).isTrue();
        assertThat(innerRan).isFalse();
        assertThat(lock.runExclusively("job", Duration.ofMinutes(1), () -> {})).isTrue();
    }

    @Test
    @DisplayName("the lock is released even when the job throws")
    void releasedOnFailure() {
        LocalClusterLock lock = new LocalClusterLock();
        try {
            lock.runExclusively("job", Duration.ofMinutes(1), () -> { throw new IllegalStateException("boom"); });
        } catch (IllegalStateException expected) {
            // expected
        }
        assertThat(lock.runExclusively("job", Duration.ofMinutes(1), () -> {})).isTrue();
    }
}
