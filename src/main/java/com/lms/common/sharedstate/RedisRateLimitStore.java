package com.lms.common.sharedstate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

/** Counts kept in Redis, shared by every backend copy. */
public class RedisRateLimitStore implements RateLimitStore {

    /** INCRBY, start the window on first use, return [count, ms left]. Atomic in Redis. */
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('INCRBY', KEYS[1], ARGV[1]) "
                    + "local ttl = redis.call('PTTL', KEYS[1]) "
                    + "if ttl < 0 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) ttl = tonumber(ARGV[2]) end "
                    + "return {c, ttl}",
            List.class);

    private final StringRedisTemplate redis;
    private final String prefix;

    public RedisRateLimitStore(StringRedisTemplate redis, String prefix) {
        this.redis = redis;
        this.prefix = prefix;
    }

    @Override
    public Hit add(String key, long delta, Duration window) {
        List<?> result = redis.execute(SCRIPT, List.of(prefix + "rl:" + key),
                String.valueOf(delta), String.valueOf(window.toMillis()));
        if (result == null || result.size() < 2) {
            throw new IllegalStateException("Unexpected reply from Redis rate-limit script");
        }
        return new Hit(((Number) result.get(0)).longValue(), ((Number) result.get(1)).longValue());
    }
}
