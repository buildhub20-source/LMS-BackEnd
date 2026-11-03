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
    private String frontendBaseUrl = "http://localhost:3000";

    public String passwordResetLink(String rawToken) {
        // The frontend serves this under the auth shell (ROUTES.RESET_PASSWORD),
        // same as the invitation link below. Without the /auth prefix the emailed
        // link resolves to no route and the reset silently dead-ends.
        return trimTrailingSlash(frontendBaseUrl) + "/auth/reset-password?token=" + rawToken;
    }

    public String invitationLink(String rawToken) {
        return trimTrailingSlash(frontendBaseUrl) + "/auth/accept-invitation?token=" + rawToken;
    }

    // Deep links used by the learning notification mails. Each one mirrors a
    // student route in the frontend's ROUTES table; keep them in step.

    public String coursePlayerLink(java.util.UUID courseId) {
        return trimTrailingSlash(frontendBaseUrl) + "/learn/" + courseId + "/player";
    }

    public String assessmentLink(java.util.UUID assessmentId) {
        return trimTrailingSlash(frontendBaseUrl) + "/learn/assessments/" + assessmentId;
    }

    public String liveClassesLink() {
        return trimTrailingSlash(frontendBaseUrl) + "/learn/live-classes";
    }

    public String liveSessionRoomLink(java.util.UUID sessionId) {
        return trimTrailingSlash(frontendBaseUrl) + "/live/" + sessionId + "/room";
    }

    /**
     * The campus toolkit has no student route of its own — students reach it
     * through the panel on their dashboard, so that is where the mail points.
     */
    public String campusToolkitLink() {
        return trimTrailingSlash(frontendBaseUrl) + "/learn/progress";
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
