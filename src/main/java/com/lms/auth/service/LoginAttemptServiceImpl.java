package com.lms.auth.service;

import com.lms.auth.entity.LoginAttempt;
import com.lms.auth.repository.LoginAttemptRepository;
import com.lms.common.audit.AuditAction;
import com.lms.common.audit.AuditService;
import com.lms.config.AuthPolicyConfig;
import com.lms.user.repository.UserRepository;
import com.lms.user.service.AccountStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final String RESOURCE = "AUTH";

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;
    private final AccountStatusService accountStatusService;
    private final AuditService auditService;
    private final AuthPolicyConfig policy;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String email, UUID userId, String ipAddress) {
        save(email, userId, ipAddress, true);
        auditService.record(userId, AuditAction.LOGIN_SUCCESS, RESOURCE, userId, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String email, UUID userId, String ipAddress) {
        save(email, userId, ipAddress, false);
        auditService.record(userId, AuditAction.LOGIN_FAILED, RESOURCE, userId,
                "Failed login for " + email);

        if (userId == null) {
            return;
        }

        long failures = recentFailureCount(email);
        if (failures >= policy.getMaxFailedAttempts()) {
            userRepository.findById(userId).ifPresent(user ->
                    accountStatusService.lock(user, null,
                            "Exceeded " + policy.getMaxFailedAttempts() + " failed login attempts"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long recentFailureCount(String email) {
        Instant windowStart = Instant.now().minus(policy.getFailedAttemptWindow());
        Instant lastSuccess = loginAttemptRepository.findLastSuccessAt(email);

        // Count from whichever is later: the start of the window, or the last
        // successful sign-in. Counting from the last success is what makes the
        // threshold consecutive rather than cumulative.
        Instant after = lastSuccess != null && lastSuccess.isAfter(windowStart) ? lastSuccess : windowStart;

        return loginAttemptRepository.countFailuresAfter(email, after);
    }

    private void save(String email, UUID userId, String ipAddress, boolean success) {
        loginAttemptRepository.save(LoginAttempt.builder()
                .userId(userId)
                .email(email)
                .ipAddress(ipAddress)
                .success(success)
                .build());
    }
}
