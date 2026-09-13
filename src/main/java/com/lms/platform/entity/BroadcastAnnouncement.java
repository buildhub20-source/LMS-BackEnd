package com.lms.platform.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "platform", name = "broadcast_announcements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastAnnouncement extends Timestamped {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String type = "INFO";

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "starts_at", nullable = false)
    @Builder.Default
    private Instant startsAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_by")
    private UUID createdBy;
}
