package com.lms.notification.service;

import com.lms.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    /** Creates and persists a notification for the given user. */
    void createNotification(UUID userId, String type, String title, String message, String link);

    /** Returns paginated notifications for the current user, newest first. */
    Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable);

    /** Returns the count of unread notifications for the user. */
    long getUnreadCount(UUID userId);

    /** Marks a single notification as read. Returns false if not found or not owned by user. */
    boolean markRead(UUID notificationId, UUID userId);

    /** Marks all notifications for the user as read. */
    int markAllRead(UUID userId);
}
