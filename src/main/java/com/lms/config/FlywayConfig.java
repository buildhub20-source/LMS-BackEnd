package com.lms.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Flyway migration strategy.
 * Migration history is immutable: automatic repair can mark missing migrations
 * as deleted, which makes tenant database provisioning non-reproducible. Repair
 * must therefore be an explicit, reviewed operational action.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.migrate();
        };
    }
}
