package com.lms.common.sharedstate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * SET NX PX lock in Redis; released only by the copy that took it.
 *
 * <p>If Redis cannot be reached the job still runs, under a lock local to this
 * copy. Skipping instead would stop reminders and institute setup for as long as
 * Redis is down. Both jobs that use this lock are safe to overlap across copies:
 * institute setup takes a row lock per tenant, and the reminder ledger has a
 * unique constraint, so the cluster lock only saves duplicated effort.
 */
@Slf4j
public class RedisClusterLock implements ClusterLock {

    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end return 0",
            Long.class);

    private final StringRedisTemplate redis;
    private final String prefix;
    private final ClusterLock fallback;

    public RedisClusterLock(StringRedisTemplate redis, String prefix) {
        this(redis, prefix, new LocalClusterLock());
    }

    RedisClusterLock(StringRedisTemplate redis, String prefix, ClusterLock fallback) {
        this.redis = redis;
        this.prefix = prefix;
        this.fallback = fallback;
    }

    @Override
    public boolean runExclusively(String name, Duration maxHold, Runnable task) {
        String key = prefix + "lock:" + name;
        String token = UUID.randomUUID().toString();
        Boolean acquired;
        try {
            acquired = redis.opsForValue().setIfAbsent(key, token, maxHold);
        } catch (RuntimeException ex) {
            log.warn("Could not reach Redis for lock {}; running under a local lock: {}", name, ex.getMessage());
            return fallback.runExclusively(name, maxHold, task);
        }
        if (!Boolean.TRUE.equals(acquired)) {
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            try {
                redis.execute(RELEASE, List.of(key), token);
            } catch (RuntimeException ex) {
                log.warn("Could not release lock {}; it expires on its own: {}", name, ex.getMessage());
            }
        }
    }
}
