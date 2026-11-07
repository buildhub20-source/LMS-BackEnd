package com.lms.common.sharedstate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Redis outage fallbacks")
class RedisFallbackTest {

    private static final Duration WINDOW = Duration.ofSeconds(60);

    private static final class MutableClock extends Clock {
        long millis;
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public long millis() { return millis; }
    }

    /** A shared store that can be switched off, and counts how often it is asked. */
    private static final class FlakyStore implements RateLimitStore {
        boolean down;
        int calls;
        final InMemoryRateLimitStore counts = new InMemoryRateLimitStore();

        @Override
        public Hit add(String key, long delta, Duration window) {
            calls++;
            if (down) throw new RedisConnectionFailureException("Redis is down");
            return counts.add(key, delta, window);
        }
    }

    @Test
    @DisplayName("counts in Redis while it answers")
    void usesSharedStore() {
        FlakyStore shared = new FlakyStore();
        FailoverRateLimitStore store = new FailoverRateLimitStore(shared, new InMemoryRateLimitStore());

        store.add("k", 1, WINDOW);
        assertThat(store.add("k", 1, WINDOW).count()).isEqualTo(2);
        assertThat(shared.calls).isEqualTo(2);
    }

    @Test
    @DisplayName("keeps counting in this process when Redis is down, instead of failing the request")
    void fallsBackWhenSharedStoreFails() {
        FlakyStore shared = new FlakyStore();
        shared.down = true;
        FailoverRateLimitStore store = new FailoverRateLimitStore(shared, new InMemoryRateLimitStore());

        assertThat(store.add("k", 1, WINDOW).count()).isEqualTo(1);
        assertThat(store.add("k", 1, WINDOW).count()).isEqualTo(2);
    }

    @Test
    @DisplayName("stops asking Redis for a while after a failure, then tries again")
    void retriesSharedStoreAfterPause() {
        MutableClock clock = new MutableClock();
        FlakyStore shared = new FlakyStore();
        shared.down = true;
        FailoverRateLimitStore store = new FailoverRateLimitStore(shared, new InMemoryRateLimitStore(), clock);

        store.add("k", 1, WINDOW);
        store.add("k", 1, WINDOW);
        assertThat(shared.calls).as("only the first request pays the timeout").isEqualTo(1);

        shared.down = false;
        clock.millis = FailoverRateLimitStore.RETRY_AFTER.toMillis();
        store.add("k", 1, WINDOW);
        assertThat(shared.calls).as("Redis is tried again after the pause").isEqualTo(2);
    }

    @Test
    @DisplayName("a scheduled job still runs, under a local lock, when Redis cannot be reached")
    @SuppressWarnings("unchecked")
    void lockRunsJobLocallyWhenRedisDown() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("Redis is down"));
        RedisClusterLock lock = new RedisClusterLock(redis, "lms:");

        AtomicInteger runs = new AtomicInteger();
        AtomicBoolean overlapRan = new AtomicBoolean();
        boolean ran = lock.runExclusively("job", Duration.ofMinutes(1), () -> {
            runs.incrementAndGet();
            overlapRan.set(lock.runExclusively("job", Duration.ofMinutes(1), runs::incrementAndGet));
        });

        assertThat(ran).isTrue();
        assertThat(runs.get()).as("the job ran once").isEqualTo(1);
        assertThat(overlapRan).as("an overlapping run in this copy is still prevented").isFalse();
    }
}
