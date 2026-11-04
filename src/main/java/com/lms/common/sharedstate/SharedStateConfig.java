package com.lms.common.sharedstate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Chooses where rate-limit counts and job locks live. Set REDIS_ENABLED=true (and
 * REDIS_URL) whenever more than one backend copy runs; otherwise in-process
 * versions are used.
 */
@Configuration
public class SharedStateConfig {

    @Configuration
    @ConditionalOnProperty(name = "lms.redis.enabled", havingValue = "true")
    static class RedisBacked {
        @Bean
        RateLimitStore redisRateLimitStore(StringRedisTemplate redis,
                                           @Value("${lms.redis.key-prefix:lms:}") String prefix) {
            return new RedisRateLimitStore(redis, prefix);
        }

        @Bean
        ClusterLock redisClusterLock(StringRedisTemplate redis,
                                     @Value("${lms.redis.key-prefix:lms:}") String prefix) {
            return new RedisClusterLock(redis, prefix);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "lms.redis.enabled", havingValue = "false", matchIfMissing = true)
    static class InProcess {
        @Bean
        RateLimitStore inMemoryRateLimitStore() {
            return new InMemoryRateLimitStore();
        }

        @Bean
        ClusterLock localClusterLock() {
            return new LocalClusterLock();
        }
    }
}
