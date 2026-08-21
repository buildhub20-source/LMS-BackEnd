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
        return trimTrailingSlash(frontendBaseUrl) + "/reset-password?token=" + rawToken;
    }

    public String invitationLink(String rawToken) {
        return trimTrailingSlash(frontendBaseUrl) + "/accept-invitation?token=" + rawToken;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
