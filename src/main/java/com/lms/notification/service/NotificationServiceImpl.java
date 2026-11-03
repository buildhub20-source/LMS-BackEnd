package com.lms.notification.service;

import com.lms.common.client.NotificationPushClient;
import com.lms.notification.dto.NotificationResponse;
import com.lms.notification.entity.Notification;
import com.lms.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPushClient notificationPushClient;

    @Override
    @Transactional
    public void createNotification(UUID userId, String type, String title, String message, String link) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .read(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.debug("Notification created for user={} type={}", userId, type);

        pushAfterCommit(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public boolean markRead(UUID notificationId, UUID userId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId).map(n -> {
            if (!n.isRead()) {
                n.setRead(true);
                n.setReadAt(Instant.now());
                notificationRepository.save(n);
            }
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllReadByUserId(userId, Instant.now());
    }

    /**
     * Hands the saved row to the notification service once the transaction has
     * committed, so the websocket never announces a notification that a
     * rollback then takes away — and never one the recipient cannot yet read.
     * Outside a transaction the push goes out immediately.
     */
    private void pushAfterCommit(Notification saved) {
        UUID tenantId = NotificationPushClient.currentTenantId();

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationPushClient.push(saved, tenantId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationPushClient.push(saved, tenantId);
            }
        });
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getLink(), n.isRead(), n.getCreatedAt(), n.getReadAt()
        );
    }
}
