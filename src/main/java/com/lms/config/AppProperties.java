package com.lms.config;

import com.lms.platform.runtime.TenantContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.net.URISyntaxException;

/** Application-level settings that are not tied to a single feature. */
@Getter
@Setter
@ConfigurationProperties(prefix = "lms.app")
public class AppProperties {

    /**
     * Base URL of the frontend, used to build invitation and reset links when no
     * institute is in scope (platform-level mail).
     */
    private String frontendBaseUrl = "http://localhost:3000";

    /**
     * The domain institutes live under (TENANT_BASE_DOMAIN). When set, links in
     * mail sent for an institute point at {@code <slug>.<domain>}: an invitation, reset
     * token or course only exists in that institute's database, so opening it on
     * any other host finds nothing.
     */
    private String tenantBaseDomain;

    public String passwordResetLink(String rawToken) {
        // The frontend serves this under the auth shell (ROUTES.RESET_PASSWORD),
        // same as the invitation link below. Without the /auth prefix the emailed
        // link resolves to no route and the reset silently dead-ends.
        return base() + "/auth/reset-password?token=" + rawToken;
    }

    public String invitationLink(String rawToken) {
        return base() + "/auth/accept-invitation?token=" + rawToken;
    }

    // Deep links used by the learning notification mails. Each one mirrors a
    // student route in the frontend's ROUTES table; keep them in step.

    public String coursePlayerLink(java.util.UUID courseId) {
        return base() + "/learn/" + courseId + "/player";
    }

    public String assessmentLink(java.util.UUID assessmentId) {
        return base() + "/learn/assessments/" + assessmentId;
    }

    public String liveClassesLink() {
        return base() + "/learn/live-classes";
    }

    public String liveSessionRoomLink(java.util.UUID sessionId) {
        return base() + "/live/" + sessionId + "/room";
    }

    /**
     * The campus toolkit has no student route of its own — students reach it
     * through the panel on their dashboard, so that is where the mail points.
     */
    public String campusToolkitLink() {
        return base() + "/learn/progress";
    }

    /** The current institute's address, or {@link #frontendBaseUrl} outside one. */
    private String base() {
        String base = trimTrailingSlash(frontendBaseUrl);
        if (tenantBaseDomain == null || tenantBaseDomain.isBlank()) {
            return base;
        }
        return TenantContext.current()
                .map(tenant -> instituteUrl(base, tenant.slug(), tenantBaseDomain))
                .orElse(base);
    }

    /** Keeps the scheme and port of {@code base}; replaces the host with {@code <slug>.<domain>}. */
    static String instituteUrl(String base, String slug, String domain) {
        URI uri = URI.create(base);
        try {
            return new URI(uri.getScheme(), null, slug + "." + domain.trim(), uri.getPort(), null, null, null).toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid frontend base URL: " + base, ex);
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
