package com.lms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Persistence infrastructure configuration.
 * Repository scanning is anchored here so that feature packages stay free of
 * infrastructure annotations.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.lms")
public class DatabaseConfig {
}
