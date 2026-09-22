package com.lms.notification.controller;

import com.lms.common.response.ApiResponse;
import com.lms.notification.dto.NotificationResponse;
import com.lms.notification.service.NotificationService;
import com.lms.security.authentication.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get paginated notifications for the current user")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(Pageable pageable) {
        UUID userId = AuthenticationService.requirePrincipal().getUserId();
        Page<NotificationResponse> page = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.of(page));
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        UUID userId = AuthenticationService.requirePrincipal().getUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.of(Map.of("count", count)));
    }

    @Operation(summary = "Mark a single notification as read")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> markRead(@PathVariable UUID id) {
        UUID userId = AuthenticationService.requirePrincipal().getUserId();
        boolean updated = notificationService.markRead(id, userId);
        return ResponseEntity.ok(ApiResponse.of(Map.of("success", updated)));
    }

    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead() {
        UUID userId = AuthenticationService.requirePrincipal().getUserId();
        int updated = notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.of(Map.of("updated", updated)));
    }
}
