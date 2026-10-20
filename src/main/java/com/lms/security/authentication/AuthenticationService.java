package com.lms.security.authentication;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Authentication infrastructure shared by the application. Feature services
 * depend on this rather than on Spring Security types directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    /** Hash checked when there is no account, so timing does not reveal whether it exists. */
    private volatile String timingDummyHash;

    /**
     * Verifies a password against a user the caller has already loaded (with
     * authorities), instead of loading it a second time. Same rules and errors
     * as {@link #authenticate(String, String)}: unknown or not-yet-activated
     * accounts and wrong passwords are INVALID_CREDENTIALS; inactive accounts are
     * ACCOUNT_DISABLED. Locked accounts are the caller's to reject first.
     */
    public LmsUserDetails authenticate(User user, String email, String rawPassword) {
        if (user == null || user.getPassword() == null) {
            passwordEncoder.matches(rawPassword, timingDummyHash());
            log.debug("Failed login attempt for {}", email);
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }
        if (!user.isActive()) {
            throw new ApplicationException(ErrorCode.ACCOUNT_DISABLED, "This account is not able to sign in");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.debug("Failed login attempt for {}", email);
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }
        return LmsUserDetails.from(user);
    }

    private String timingDummyHash() {
        String hash = timingDummyHash;
        if (hash == null) {
            hash = passwordEncoder.encode("timing-equalisation-only");
            timingDummyHash = hash;
        }
        return hash;
    }

    /**
     * Verifies credentials and returns the resolved principal.
     *
     * @throws ApplicationException with INVALID_CREDENTIALS or ACCOUNT_DISABLED
     */
    public LmsUserDetails authenticate(String email, String rawPassword) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, rawPassword));
            return (LmsUserDetails) authentication.getPrincipal();
        } catch (DisabledException | LockedException ex) {
            throw new ApplicationException(ErrorCode.ACCOUNT_DISABLED,
                    "This account is not able to sign in");
        } catch (AuthenticationException ex) {
            log.debug("Failed login attempt for {}", email);
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }
    }

    /** The principal bound to the current request, if any. */
    public static Optional<LmsUserDetails> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LmsUserDetails principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    /** The principal bound to the current request, or an authentication error. */
    public static LmsUserDetails requirePrincipal() {
        return currentPrincipal().orElseThrow(() ->
                new ApplicationException(ErrorCode.UNAUTHENTICATED, "No authenticated user in context"));
    }
}
