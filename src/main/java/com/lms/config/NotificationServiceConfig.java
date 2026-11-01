package com.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for outbound calls to the LMS-Notification-Service.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "lms.notification-service")
public class NotificationServiceConfig {

    /**
     * Base URL of the notification microservice.
     * Example: http://notification-service:3002
     */
    private String baseUrl = "http://localhost:3002";

    /**
     * When false, notifications are still persisted but no socket push is
     * attempted. The bell then only updates when the client refetches.
     */
    private boolean enabled = false;

    /**
     * Timeout in milliseconds for the push call. Failures are non-fatal, so a
     * short timeout is preferred over blocking the caller.
     */
    private int timeoutMs = 2000;
}
