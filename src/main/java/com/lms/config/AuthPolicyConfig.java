package com.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Authentication policy: lockout thresholds and token lifetimes for the
 * invitation and password reset flows.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "lms.security.policy")
public class AuthPolicyConfig {

    /** Consecutive failures within the window before the account is locked. */
    private int maxFailedAttempts = 5;

    /** Sliding window over which failed attempts are counted. */
    private Duration failedAttemptWindow = Duration.ofMinutes(15);

    /** Lifetime of an invitation token. */
    private Duration invitationTtl = Duration.ofDays(7);

    /** Lifetime of a password reset token. */
    private Duration passwordResetTtl = Duration.ofMinutes(30);

    /** Maximum number of concurrent sessions retained per user. */
    private int maxSessionsPerUser = 10;
}
