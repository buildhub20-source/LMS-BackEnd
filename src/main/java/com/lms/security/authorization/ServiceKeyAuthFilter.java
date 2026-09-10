package com.lms.security.authorization;

import com.lms.config.InternalApiConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Protects {@code /api/v1/internal/**} endpoints from external callers.
 *
 * <p>Any request whose path starts with the internal prefix must carry the
 * {@code X-Service-Key} header with the value that matches
 * {@code lms.internal.service-key}. Requests without the header, or with
 * an incorrect value, receive a plain 401 response — no body that could leak
 * information about the expected value.
 *
 * <p>This filter is registered <em>before</em> the JWT filter so that the
 * internal routes never reach JWT validation.
 */
@Slf4j
@RequiredArgsConstructor
public class ServiceKeyAuthFilter extends OncePerRequestFilter {

    public static final String SERVICE_KEY_HEADER = "X-Service-Key";
    public static final String INTERNAL_PATH_PREFIX = "/api/v1/internal/";
    /** Authority granted only after a valid service-to-service key is verified. */
    public static final String INTERNAL_SERVICE_AUTHORITY = "INTERNAL_SERVICE";

    private final InternalApiConfig internalApiConfig;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String providedKey = request.getHeader(SERVICE_KEY_HEADER);

        if (!internalApiConfig.getServiceKey().equals(providedKey)) {
            log.warn("Rejected internal API call from {} — invalid or missing service key",
                    request.getRemoteAddr());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }

        // Do not rely on this filter's short-circuit alone. Establish an
        // authenticated principal so Spring Security's authorization rules
        // also reject an internal route if filter ordering changes later.
        var authentication = new UsernamePasswordAuthenticationToken(
                "internal-service",
                null,
                List.of(new SimpleGrantedAuthority(INTERNAL_SERVICE_AUTHORITY)));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
