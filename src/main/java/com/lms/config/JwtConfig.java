package com.lms.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * JWT settings supplied through environment configuration.
 * The signing secret must never be committed to source control.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "lms.security.jwt")
public class JwtConfig {

    /** HMAC signing secret. Must be at least 32 bytes for HS256. */
    @NotBlank
    private String secret;

    /** Value placed in the iss claim. */
    private String issuer = "lms-backend";

    /** Lifetime of an access token. */
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    /** Lifetime of a refresh token, and therefore of a session. */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /** Tolerance applied when validating exp and nbf. */
    private Duration clockSkew = Duration.ofSeconds(30);
}
