package com.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Application-level settings that are not tied to a single feature. */
@Getter
@Setter
@ConfigurationProperties(prefix = "lms.app")
public class AppProperties {

    /** Base URL of the frontend, used to build invitation and reset links. */
    private String frontendBaseUrl = "http://localhost:5173";

    public String passwordResetLink(String rawToken) {
        // The frontend serves this under the auth shell (ROUTES.RESET_PASSWORD),
        // same as the invitation link below. Without the /auth prefix the emailed
        // link resolves to no route and the reset silently dead-ends.
        return trimTrailingSlash(frontendBaseUrl) + "/auth/reset-password?token=" + rawToken;
    }

    public String invitationLink(String rawToken) {
        return trimTrailingSlash(frontendBaseUrl) + "/auth/accept-invitation?token=" + rawToken;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
