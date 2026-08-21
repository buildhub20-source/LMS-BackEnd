package com.lms.auth.service;

import com.lms.auth.dto.request.LoginRequest;
import com.lms.auth.dto.request.LogoutRequest;
import com.lms.auth.dto.request.RefreshTokenRequest;
import com.lms.auth.dto.response.AuthTokens;
import com.lms.auth.dto.response.CurrentUserResponse;
import com.lms.auth.dto.response.LoginResponse;
import com.lms.auth.dto.response.SessionResponse;
import com.lms.auth.entity.UserSession;
import com.lms.auth.mapper.AuthMapper;
import com.lms.common.audit.AuditAction;
import com.lms.common.audit.AuditService;
import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.util.HttpRequests;
import com.lms.security.authentication.AuthenticationService;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.security.jwt.JwtService;
import com.lms.user.entity.User;
import com.lms.user.mapper.UserMapper;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Authentication flows.
 *
 * <p>Login is deliberately not a single transaction: the attempt record and any
 * resulting lock are written by {@link LoginAttemptService} in their own
 * transactions so they survive the failure path.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String RESOURCE = "AUTH";

    private final AuthenticationService authenticationService;
    private final SessionService sessionService;
    private final LoginAttemptService loginAttemptService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String ipAddress = HttpRequests.clientIp();

        Optional<User> candidate = userRepository.findByEmailWithAuthorities(email);
        UUID candidateId = candidate.map(User::getId).orElse(null);

        // Checked before credentials so a locked account cannot be probed by
        // password guessing, and so the lock is reported consistently.
        if (candidate.isPresent() && candidate.get().isLocked()) {
            loginAttemptService.recordFailure(email, candidateId, ipAddress);
            throw new ApplicationException(ErrorCode.ACCOUNT_DISABLED,
                    "This account is locked. Contact an administrator.");
        }

        LmsUserDetails principal;
        try {
            principal = authenticationService.authenticate(email, request.getPassword());
        } catch (ApplicationException ex) {
            loginAttemptService.recordFailure(email, candidateId, ipAddress);
            throw ex;
        }

        User user = candidate.orElseThrow(() ->
                new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

        loginAttemptService.recordSuccess(email, user.getId(), ipAddress);

        SessionService.IssuedSession session = sessionService.openSession(user);

        String accessToken = jwtService.generateAccessToken(principal, session.getSessionId());

        log.info("User {} signed in", email);

        AuthTokens tokens = AuthTokens.bearer(accessToken, session.getRawRefreshToken(),
                jwtService.accessTokenTtlSeconds());

        return new LoginResponse(tokens, userMapper.toResponse(user), false);
    }

    @Override
    @Transactional
    public AuthTokens refresh(RefreshTokenRequest request) {
        SessionService.RotatedSession rotated = sessionService.rotate(request.getRefreshToken());
        UserSession session = rotated.getSession();

        String accessToken = jwtService.generateAccessToken(
                LmsUserDetails.from(session.getUser()), session.getId());

        auditService.record(session.getUser().getId(), AuditAction.TOKEN_REFRESHED,
                RESOURCE, session.getId(), null);

        return AuthTokens.bearer(accessToken, rotated.getRawRefreshToken(), jwtService.accessTokenTtlSeconds());
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();

        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            sessionService.revokeByRefreshToken(request.getRefreshToken());
        } else if (principal.getSessionId() != null) {
            sessionService.revokeById(principal.getSessionId(), principal.getUserId(), false);
        }

        auditService.record(principal.getUserId(), AuditAction.LOGOUT, RESOURCE, principal.getUserId(), null);
        log.info("User {} signed out", principal.getEmail());
    }

    @Override
    @Transactional
    public int logoutEverywhere() {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        int revoked = sessionService.revokeAllForUser(principal.getUserId());
        auditService.record(principal.getUserId(), AuditAction.LOGOUT, RESOURCE, principal.getUserId(),
                "Signed out of all sessions");
        return revoked;
    }


    @Override
    public CurrentUserResponse currentUser() {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();

        User user = userRepository.findByIdWithAuthorities(principal.getUserId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.UNAUTHENTICATED,
                        "The authenticated account no longer exists"));

        return authMapper.toCurrentUser(user);
    }

    @Override
    public List<SessionResponse> mySessions() {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        return authMapper.toSessionResponses(
                sessionService.activeSessions(principal.getUserId()), principal.getSessionId());
    }

    @Override
    @Transactional
    public void revokeSession(UUID sessionId) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        sessionService.revokeById(sessionId, principal.getUserId(), false);
    }
}
