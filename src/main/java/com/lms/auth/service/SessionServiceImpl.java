package com.lms.auth.service;

import com.lms.auth.entity.UserSession;
import com.lms.auth.repository.UserSessionRepository;
import com.lms.common.audit.AuditAction;
import com.lms.common.audit.AuditService;
import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.exception.InvalidTokenException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.util.HttpRequests;
import com.lms.common.util.TokenGenerator;
import com.lms.common.util.TokenHasher;
import com.lms.config.AuthPolicyConfig;
import com.lms.config.JwtConfig;
import com.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionServiceImpl implements SessionService {

    private static final String RESOURCE = "SESSION";

    private final UserSessionRepository sessionRepository;
    private final AuditService auditService;
    private final JwtConfig jwtConfig;
    private final AuthPolicyConfig policy;

    @Override
    @Transactional
    public IssuedSession openSession(User user) {
        enforceSessionCap(user.getId());

        String rawToken = TokenGenerator.urlSafeToken();

        UserSession session = UserSession.builder()
                .user(user)
                .refreshTokenHash(TokenHasher.sha256(rawToken))
                .ipAddress(HttpRequests.clientIp())
                .userAgent(HttpRequests.userAgent())
                .revoked(false)
                .expiresAt(Instant.now().plus(jwtConfig.getRefreshTokenTtl()))
                .lastUsedAt(Instant.now())
                .build();

        UserSession saved = sessionRepository.save(session);
        log.debug("Opened session {} for {}", saved.getId(), user.getEmail());

        return new IssuedSession(saved.getId(), rawToken);
    }

    @Override
    @Transactional
    public RotatedSession rotate(String rawRefreshToken) {
        UserSession session = sessionRepository.findByRefreshTokenHash(TokenHasher.sha256(rawRefreshToken))
                .orElseThrow(() -> new InvalidTokenException("Refresh token is not valid"));

        Instant now = Instant.now();

        if (!session.isUsable(now)) {
            // A revoked or expired session must not be resurrected. Revoking
            // again is harmless and keeps the outcome identical either way.
            session.setRevoked(true);
            throw new InvalidTokenException("Refresh token is no longer valid");
        }

        if (!session.getUser().canAuthenticate()) {
            session.setRevoked(true);
            throw new ApplicationException(ErrorCode.ACCOUNT_DISABLED, "This account can no longer sign in");
        }

        String replacement = TokenGenerator.urlSafeToken();
        session.setRefreshTokenHash(TokenHasher.sha256(replacement));
        session.setLastUsedAt(now);

        return new RotatedSession(session, replacement);
    }

    @Override
    @Transactional
    public void revokeByRefreshToken(String rawRefreshToken) {
        sessionRepository.findByRefreshTokenHash(TokenHasher.sha256(rawRefreshToken))
                .ifPresent(this::revoke);
    }

    @Override
    @Transactional
    public void revokeById(UUID sessionId, UUID requestingUserId, boolean allowAnyUser) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Session", sessionId));

        if (!allowAnyUser && !session.getUser().getId().equals(requestingUserId)) {
            // Reported as not found rather than forbidden so session ids of
            // other users cannot be probed.
            throw ResourceNotFoundException.of("Session", sessionId);
        }
        revoke(session);
    }

    @Override
    @Transactional
    public int revokeAllForUser(UUID userId) {
        int revoked = sessionRepository.revokeAllForUser(userId);
        if (revoked > 0) {
            auditService.record(userId, AuditAction.SESSION_REVOKED, RESOURCE, userId,
                    "Revoked " + revoked + " session(s)");
        }
        return revoked;
    }

    @Override
    public List<UserSession> activeSessions(UUID userId) {
        return sessionRepository.findAllByUserIdAndRevokedFalseOrderByLastUsedAtDesc(userId).stream()
                .filter(session -> !session.isExpired(Instant.now()))
                .toList();
    }

    @Override
    @Transactional
    public int purgeExpired() {
        return sessionRepository.deleteExpiredBefore(Instant.now());
    }

    private void revoke(UserSession session) {
        if (session.isRevoked()) {
            return;
        }
        session.setRevoked(true);
        auditService.record(session.getUser().getId(), AuditAction.SESSION_REVOKED,
                RESOURCE, session.getId(), null);
    }

    /**
     * Keeps the number of live sessions bounded. Without this, a client that
     * logs in on every request would grow the table without limit.
     */
    private void enforceSessionCap(UUID userId) {
        List<UserSession> active = sessionRepository.findAllByUserIdAndRevokedFalseOrderByLastUsedAtDesc(userId);
        int surplus = active.size() - policy.getMaxSessionsPerUser() + 1;

        if (surplus <= 0) {
            return;
        }

        active.stream()
                .sorted(Comparator.comparing(UserSession::getLastUsedAt))
                .limit(surplus)
                .forEach(session -> session.setRevoked(true));

        log.debug("Revoked {} oldest session(s) for user {} to stay within the cap", surplus, userId);
    }
}
