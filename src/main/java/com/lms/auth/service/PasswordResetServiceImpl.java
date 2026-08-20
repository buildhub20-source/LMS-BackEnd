package com.lms.auth.service;

import com.lms.auth.dto.request.ForgotPasswordRequest;
import com.lms.auth.dto.request.ResetPasswordRequest;
import com.lms.auth.entity.PasswordResetToken;
import com.lms.auth.repository.PasswordResetTokenRepository;
import com.lms.common.audit.AuditAction;
import com.lms.common.audit.AuditService;
import com.lms.common.exception.InvalidTokenException;
import com.lms.common.mail.MailMessage;
import com.lms.common.mail.MailSender;
import com.lms.common.util.TokenGenerator;
import com.lms.common.util.TokenHasher;
import com.lms.config.AppProperties;
import com.lms.config.AuthPolicyConfig;
import com.lms.user.entity.User;
import com.lms.user.event.PasswordChangedEvent;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String RESOURCE = "AUTH";

    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;
    private final AuditService auditService;
    private final AppProperties appProperties;
    private final AuthPolicyConfig policy;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> candidate = userRepository.findByEmailIgnoreCase(email);

        if (candidate.isEmpty()) {
            log.debug("Password reset requested for unknown address {}", email);
            return;
        }

        User user = candidate.get();
        if (user.getPassword() == null) {
            // The account has never been activated; the invitation flow, not
            // the reset flow, is what sets its first password.
            log.debug("Password reset requested for a never-activated account {}", email);
            return;
        }

        // A new request supersedes any token still outstanding.
        resetTokenRepository.invalidateAllForUser(user.getId());

        String rawToken = TokenGenerator.urlSafeToken();

        resetTokenRepository.save(PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(TokenHasher.sha256(rawToken))
                .expiresAt(Instant.now().plus(policy.getPasswordResetTtl()))
                .used(false)
                .build());

        mailSender.send(new MailMessage(user.getEmail(), "Reset your LMS password", resetBody(user, rawToken)));

        auditService.record(user.getId(), AuditAction.PASSWORD_RESET_REQUESTED, RESOURCE, user.getId(), null);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(TokenHasher.sha256(request.getToken()))
                .orElseThrow(() -> new InvalidTokenException("Password reset token is not valid"));

        if (!token.isRedeemable(Instant.now())) {
            throw new InvalidTokenException("Password reset token is no longer valid");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidTokenException("Password reset token is not valid"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        token.setUsed(true);

        // A password change ends every session: whoever triggered the reset may
        // not be the party holding the existing refresh tokens.
        sessionService.revokeAllForUser(user.getId());

        eventPublisher.publishEvent(new PasswordChangedEvent(user.getId()));

        auditService.record(user.getId(), AuditAction.PASSWORD_RESET, RESOURCE, user.getId(), null);
        log.info("Password reset completed for {}", user.getEmail());
    }

    private String resetBody(User user, String rawToken) {
        return """
                Hello %s,

                A password reset was requested for your LMS account.

                Reset your password: %s

                This link expires in %d minutes and can be used once. If you did
                not request it, no action is needed.
                """.formatted(
                user.getName(),
                appProperties.passwordResetLink(rawToken),
                policy.getPasswordResetTtl().toMinutes());
    }
}
