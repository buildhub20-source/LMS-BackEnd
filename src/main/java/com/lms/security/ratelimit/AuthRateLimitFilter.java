package com.lms.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.common.constants.ApiPaths;
import com.lms.common.exception.ErrorCode;
import com.lms.common.response.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.lms.common.sharedstate.RateLimitStore;

import java.io.IOException;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Fixed-window, per-client-IP limit on the public authentication endpoints and
 * on public certificate verification.
 * Runs after Spring's forwarded-header handling, so behind the host's proxy
 * the remote address is the real client. Counts live in {@link RateLimitStore},
 * which is Redis when more than one backend copy runs.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter implements Ordered {

    private final AuthRateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final RateLimitStore store;
    private final Map<String, ToIntFunction<AuthRateLimitProperties>> limits = Map.of(
            ApiPaths.AUTH + "/login", AuthRateLimitProperties::getLogin,
            ApiPaths.AUTH + "/refresh", AuthRateLimitProperties::getRefresh,
            ApiPaths.AUTH + "/forgot-password", AuthRateLimitProperties::getForgotPassword,
            ApiPaths.AUTH + "/reset-password", AuthRateLimitProperties::getResetPassword,
            ApiPaths.AUTH + "/accept-invitation", AuthRateLimitProperties::getAcceptInvitation,
            ApiPaths.PLATFORM + "/auth/login", AuthRateLimitProperties::getPlatformLogin);
    /** Every serial and token is its own path, so verification shares one window per client. */
    private static final String CERTIFICATE_VERIFY_PREFIX = ApiPaths.API_V1 + "/certificates/verify/";

    public AuthRateLimitFilter(AuthRateLimitProperties properties, ObjectMapper objectMapper, RateLimitStore store) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.store = store;
    }

    @Override
    public int getOrder() {
        // ForwardedHeaderFilter runs at HIGHEST_PRECEDENCE; this must come after it.
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled() || limitKey(request) == null;
    }

    /** The window a request counts against, or null when it is not limited. */
    private String limitKey(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod()) && limits.containsKey(path)) {
            return path;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && path.startsWith(CERTIFICATE_VERIFY_PREFIX)) {
            return CERTIFICATE_VERIFY_PREFIX;
        }
        return null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String path = limitKey(request);
        int limit = CERTIFICATE_VERIFY_PREFIX.equals(path)
                ? properties.getCertificateVerify()
                : limits.get(path).applyAsInt(properties);

        RateLimitStore.Hit hit = store.add("auth:" + request.getRemoteAddr() + '|' + path, 1, properties.getWindow());
        if (hit.count() > limit) {
            long retryAfterSeconds = Math.max(1, (hit.resetsInMillis() + 999) / 1000);
            reject(request, response, retryAfterSeconds);
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        response.setStatus(ErrorCode.TOO_MANY_REQUESTS.status().value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(ErrorCode.TOO_MANY_REQUESTS.name(),
                "Too many attempts. Try again in " + retryAfterSeconds + " seconds.", request.getRequestURI()));
    }
}
