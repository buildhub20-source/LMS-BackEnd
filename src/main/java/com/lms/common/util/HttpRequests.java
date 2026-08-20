package com.lms.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Reads client metadata from the request bound to the current thread.
 *
 * <p>{@code X-Forwarded-For} is only trusted when the deployment sits behind a
 * proxy that overwrites it; see {@code server.forward-headers-strategy}.
 */
public final class HttpRequests {

    private static final String UNKNOWN_IP = "unknown";
    private static final int MAX_USER_AGENT = 255;

    private HttpRequests() {
    }

    public static Optional<HttpServletRequest> current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return Optional.of(attributes.getRequest());
        }
        return Optional.empty();
    }

    public static String clientIp() {
        return current().map(HttpRequests::clientIp).orElse(UNKNOWN_IP);
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String first = forwarded.split(",")[0].trim();
            if (StringUtils.hasText(first)) {
                return truncate(first, 45);
            }
        }
        String remote = request.getRemoteAddr();
        return StringUtils.hasText(remote) ? truncate(remote, 45) : UNKNOWN_IP;
    }

    public static String userAgent() {
        return current()
                .map(request -> request.getHeader("User-Agent"))
                .filter(StringUtils::hasText)
                .map(value -> truncate(value, MAX_USER_AGENT))
                .orElse(null);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
