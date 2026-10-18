package com.lms.security.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Per-client-IP request limits on the public authentication endpoints.
 *
 * <p>Institutes often put a whole campus behind one public IP, so sign-in and
 * token refresh are deliberately more generous than the endpoints that send
 * email. The per-account lockout still protects individual passwords.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "lms.security.rate-limit")
public class AuthRateLimitProperties {
    private boolean enabled = true;
    private Duration window = Duration.ofMinutes(1);
    private int login = 60;
    private int refresh = 120;
    private int forgotPassword = 5;
    private int resetPassword = 10;
    private int acceptInvitation = 10;
    private int platformLogin = 10;
}
