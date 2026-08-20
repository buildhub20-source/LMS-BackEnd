package com.lms.security.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.common.constants.ApiPaths;
import com.lms.common.constants.SecurityConstants;
import com.lms.common.exception.ErrorCode;
import com.lms.common.response.ApiError;
import com.lms.security.authentication.LmsUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Confines a principal still holding a temporary password to the endpoints it
 * needs in order to stop holding one.
 *
 * <p>Such a token already carries no roles and no permissions, so every
 * {@code @PreAuthorize} endpoint denies it on its own. This filter closes the
 * remaining gap: the handful of endpoints that only require authentication.
 * Stated as an explicit allowlist because the failure mode of getting it wrong
 * is an un-onboarded account acting as a real one.
 */
@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            ApiPaths.USERS + "/me/password",
            ApiPaths.AUTH + "/me",
            ApiPaths.AUTH + "/logout",
            ApiPaths.AUTH + "/logout-all"
    );

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!onboardingIncomplete() || ALLOWED_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), ApiError.of(
                ErrorCode.PASSWORD_CHANGE_REQUIRED.name(),
                "Set a password of your own before using the rest of the API",
                request.getRequestURI()));
    }

    private boolean onboardingIncomplete() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.getPrincipal() instanceof LmsUserDetails principal
                && principal.getPermissions().contains(SecurityConstants.PASSWORD_CHANGE_ONLY);
    }
}
