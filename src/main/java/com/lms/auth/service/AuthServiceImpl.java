package com.lms.auth.service;

import com.lms.auth.dto.request.AcceptInvitationRequest;
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
import com.lms.common.constants.SecurityConstants;
import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.util.HttpRequests;
import com.lms.common.util.TokenHasher;
import com.lms.invitation.entity.Invitation;
import com.lms.invitation.repository.InvitationRepository;
import com.lms.security.authentication.AuthenticationService;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.security.jwt.JwtService;
import com.lms.user.entity.User;
import com.lms.user.entity.AccountStatus;
import com.lms.user.mapper.UserMapper;
import com.lms.user.repository.UserRepository;
import com.lms.user.service.AccountStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.Optional;
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
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountStatusService accountStatusService;

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

        rejectExpiredTemporaryPassword(candidate.orElse(null), email, ipAddress);

        User user = candidate.orElseThrow(() ->
                new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

        boolean mustChangePassword = onboardingOutstanding(user);

        loginAttemptService.recordSuccess(email, user.getId(), ipAddress);

        SessionService.IssuedSession session = sessionService.openSession(user);

        // While the temporary password is still in force the token carries no
        // roles and no permissions, so every guarded endpoint denies it and
        // PasswordChangeRequiredFilter closes off the rest.
        LmsUserDetails tokenPrincipal = mustChangePassword ? onboardingPrincipal(user) : principal;
        String accessToken = jwtService.generateAccessToken(tokenPrincipal, session.getSessionId());

        log.info("User {} signed in{}", email, mustChangePassword ? " (password change required)" : "");

        AuthTokens tokens = AuthTokens.bearer(accessToken, session.getRawRefreshToken(),
                jwtService.accessTokenTtlSeconds());

        return new LoginResponse(tokens, userMapper.toResponse(user), mustChangePassword);
    }

    @Override
    @Transactional
    public LoginResponse acceptInvitation(AcceptInvitationRequest request) {
        String tokenHash = TokenHasher.sha256(request.getToken());

        Invitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_CREDENTIALS,
                        "Invalid or expired invitation link."));

        if (invitation.isAccepted()) {
            throw new BusinessRuleException(
                    "This invitation has already been accepted. Please sign in instead.");
        }

        if (invitation.isExpired(Instant.now())) {
            throw new ApplicationException(ErrorCode.ACCOUNT_DISABLED,
                    "This invitation link has expired. Ask an administrator to resend your invitation.");
        }

        User user = invitation.getUser();

        // Set the permanent password and activate the account
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setActive(true);

        // Stamp the invitation as accepted
        invitation.setAcceptedAt(Instant.now());

        // Record the ACTIVE status transition
        accountStatusService.recordTransition(user.getId(), AccountStatus.ACTIVE, null,
                "Account activated via invitation magic link");

        // Open a full session — the user is now fully onboarded
        LmsUserDetails principal = LmsUserDetails.from(user);
        SessionService.IssuedSession session = sessionService.openSession(user);
        String accessToken = jwtService.generateAccessToken(principal, session.getSessionId());
        AuthTokens tokens = AuthTokens.bearer(accessToken, session.getRawRefreshToken(),
                jwtService.accessTokenTtlSeconds());

        auditService.record(user.getId(), AuditAction.USER_INVITED, "AUTH", invitation.getId(),
                "Invitation accepted by " + user.getEmail());

        log.info("Invitation accepted and account activated for {}", user.getEmail());

        return new LoginResponse(tokens, userMapper.toResponse(user), false);
    }

    @Override
    @Transactional
    public AuthTokens refresh(RefreshTokenRequest request) {
        SessionService.RotatedSession rotated = sessionService.rotate(request.getRefreshToken());
        UserSession session = rotated.getSession();

        // Refresh must not be a way to trade a restricted token for a full one.
        LmsUserDetails principal = onboardingOutstanding(session.getUser())
                ? onboardingPrincipal(session.getUser())
                : LmsUserDetails.from(session.getUser());

        String accessToken = jwtService.generateAccessToken(principal, session.getId());

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

    /**
     * An expired invitation takes the temporary password with it, otherwise the
     * credential would outlive the window it was issued for.
     */
    private void rejectExpiredTemporaryPassword(User user, String email, String ipAddress) {
        if (user == null) {
            return;
        }
        Invitation pending = invitationRepository
                .findFirstByUserIdAndAcceptedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElse(null);

        if (pending != null && pending.isExpired(Instant.now())) {
            loginAttemptService.recordFailure(email, user.getId(), ipAddress);
            throw new ApplicationException(ErrorCode.ACCOUNT_DISABLED,
                    "Your temporary password has expired. Ask an administrator to resend your invitation.");
        }
    }

    /**
     * True while the account is still on the temporary password issued at
     * invite time. An outstanding invitation is what records that.
     */
    private boolean onboardingOutstanding(User user) {
        return invitationRepository
                .findFirstByUserIdAndAcceptedAtIsNullOrderByCreatedAtDesc(user.getId())
                .isPresent();
    }

    /** A principal that can do nothing except replace its own password. */
    private LmsUserDetails onboardingPrincipal(User user) {
        return new LmsUserDetails(user.getId(), user.getEmail(), user.getName(), null,
                user.isActive(), user.isLocked(),
                Set.of(), Set.of(SecurityConstants.PASSWORD_CHANGE_ONLY), null);
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
