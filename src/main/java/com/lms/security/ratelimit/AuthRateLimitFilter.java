package com.lms.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.common.constants.ApiPaths;
import com.lms.common.exception.ErrorCode;
import com.lms.common.response.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToIntFunction;

/**
 * Fixed-window, per-client-IP limit on the public authentication endpoints.
 * Runs after Spring's forwarded-header handling, so behind the host's proxy
 * the remote address is the real client.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter implements Ordered {

    private static final int PRUNE_THRESHOLD = 10_000;

    private final AuthRateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, ToIntFunction<AuthRateLimitProperties>> limits = Map.of(
            ApiPaths.AUTH + "/login", AuthRateLimitProperties::getLogin,
            ApiPaths.AUTH + "/refresh", AuthRateLimitProperties::getRefresh,
            ApiPaths.AUTH + "/forgot-password", AuthRateLimitProperties::getForgotPassword,
            ApiPaths.AUTH + "/reset-password", AuthRateLimitProperties::getResetPassword,
            ApiPaths.AUTH + "/accept-invitation", AuthRateLimitProperties::getAcceptInvitation,
            ApiPaths.PLATFORM + "/auth/login", AuthRateLimitProperties::getPlatformLogin);
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public AuthRateLimitFilter(AuthRateLimitProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    AuthRateLimitFilter(AuthRateLimitProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public int getOrder() {
        // ForwardedHeaderFilter runs at HIGHEST_PRECEDENCE; this must come after it.
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled()
                || !"POST".equalsIgnoreCase(request.getMethod())
                || !limits.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = limits.get(path).applyAsInt(properties);
        long now = clock.millis();
        long windowMillis = properties.getWindow().toMillis();

        if (windows.size() > PRUNE_THRESHOLD) {
            windows.values().removeIf(w -> now - w.start >= windowMillis);
        }

        Window window = windows.compute(request.getRemoteAddr() + '|' + path, (key, current) ->
                current == null || now - current.start >= windowMillis ? new Window(now) : current);
        int count;
        synchronized (window) {
            count = ++window.count;
        }

        if (count > limit) {
            long retryAfterSeconds = Math.max(1, (window.start + windowMillis - now + 999) / 1000);
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

    private static final class Window {
        private final long start;
        private int count;

        private Window(long start) {
            this.start = start;
        }
    }
}
