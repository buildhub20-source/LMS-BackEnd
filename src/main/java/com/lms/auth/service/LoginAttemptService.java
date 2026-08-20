package com.lms.auth.service;

import java.util.UUID;

/**
 * Records login attempts and enforces the lockout policy.
 *
 * <p>Every method runs in its own transaction so that the attempt is still
 * recorded when the surrounding login transaction rolls back on failure.
 */
public interface LoginAttemptService {

    void recordSuccess(String email, UUID userId, String ipAddress);

    /**
     * Records a failed attempt and locks the account when the consecutive
     * failure threshold is reached.
     *
     * @param userId the matched account, or null when the address is unknown
     */
    void recordFailure(String email, UUID userId, String ipAddress);

    /** Consecutive failures for this address inside the policy window. */
    long recentFailureCount(String email);
}
