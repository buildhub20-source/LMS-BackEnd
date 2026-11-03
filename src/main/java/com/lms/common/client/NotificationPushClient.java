package com.lms.common.client;

import com.lms.config.InternalApiConfig;
import com.lms.config.NotificationServiceConfig;
import com.lms.notification.entity.Notification;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import com.lms.security.authorization.ServiceKeyAuthFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fire-and-forget HTTP client that asks the LMS-Notification-Service to push a
 * notification over its websocket.
 *
 * <p>The row itself is written here, by JPA, inside the transaction that raised
 * it — both services read the same {@code lms.notifications} table. This client
 * only covers the real-time half, which is why it targets the push endpoint
 * rather than the one that inserts: calling that would duplicate every row.
 *
 * <p>{@link Async} because a socket push must never delay or fail the change
 * that triggered it. If the notification service is down the row is still
 * there, and the bell catches up on the client's next refetch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPushClient {

    private final NotificationServiceConfig notificationConfig;
    private final InternalApiConfig internalApiConfig;
    private final RestTemplate internalRestTemplate;

    /**
     * Pushes an already-persisted notification to its recipient's socket room.
     *
     * @param notification the committed notification row
     * @param tenantId     tenant whose database holds that row; the notification
     *                     service needs it to pick the right pool and socket room
     */
    @Async
    public void push(Notification notification, UUID tenantId) {
        if (!notificationConfig.isEnabled()) {
            log.debug("Notification service integration is disabled — skipping push for notification={}",
                    notification.getId());
            return;
        }
        if (tenantId == null) {
            log.warn("No tenant in scope — skipping push for notification={}", notification.getId());
            return;
        }

        try {
            String url = notificationConfig.getBaseUrl() + "/api/v1/internal/notifications/push";

            HttpHeaders headers = new HttpHeaders();
            headers.set(ServiceKeyAuthFilter.SERVICE_KEY_HEADER, internalApiConfig.getServiceKey());
            headers.set("Content-Type", "application/json");

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", notification.getId().toString());
            payload.put("userId", notification.getUserId().toString());
            payload.put("type", notification.getType());
            payload.put("title", notification.getTitle());
            // Both keys: the socket payload mirrors the notification service's
            // own row mapping, which exposes the text under each name.
            payload.put("message", notification.getMessage());
            payload.put("body", notification.getMessage());
            payload.put("linkUrl", notification.getLink());
            payload.put("isRead", notification.isRead());
            payload.put("createdAt", notification.getCreatedAt());

            Map<String, Object> body = new HashMap<>();
            body.put("tenantId", tenantId.toString());
            body.put("userId", notification.getUserId().toString());
            body.put("notification", payload);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            internalRestTemplate.exchange(url, HttpMethod.POST, request, Void.class);

            log.info("Pushed notification {} to user {}", notification.getId(), notification.getUserId());

        } catch (Exception ex) {
            // Non-fatal — the notification is already committed and readable.
            log.warn("Failed to push notification {} to the notification service: {}",
                    notification.getId(), ex.getMessage());
        }
    }

    /** Tenant currently in scope, or null outside a tenant-resolved request. */
    public static UUID currentTenantId() {
        return TenantContext.current().map(TenantConnection::tenantId).orElse(null);
    }
}
