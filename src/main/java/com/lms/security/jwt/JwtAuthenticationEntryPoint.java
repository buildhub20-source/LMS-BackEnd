package com.lms.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.common.exception.ErrorCode;
import com.lms.common.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns the standard error contract for unauthenticated requests instead of
 * the default HTML error page.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError body = ApiError.of(ErrorCode.UNAUTHENTICATED.name(),
                "Authentication is required to access this resource", request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
