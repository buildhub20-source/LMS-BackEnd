package com.lms.announcement.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An institute-wide message from an instructor or admin. It is shown to
 * students while it is active and not past {@code expiresAt}.
 */
@Entity
@Table(name = "announcements")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Announcement extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AnnouncementType type;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_by")
    private UUID createdBy;

    /** True while students should see it. */
    public boolean isVisibleAt(Instant now) {
        return active && (expiresAt == null || expiresAt.isAfter(now));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Announcement a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
