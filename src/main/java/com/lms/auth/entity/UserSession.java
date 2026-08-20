package com.lms.auth.entity;

import com.lms.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A refresh-token session.
 *
 * <p>The session id is the token family: rotation replaces
 * {@code refresh_token_hash} in place, so exactly one refresh token is valid
 * per session at any moment. Revoking the row ends the family.
 *
 * <p>Only the SHA-256 digest of the refresh token is stored.
 */
@Entity
@Table(name = "user_session",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_session_refresh_token_hash",
                columnNames = "refresh_token_hash"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_session_user"))
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Builder.Default
    @Column(name = "is_revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt = Instant.now();

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public boolean isUsable(Instant now) {
        return !revoked && !isExpired(now);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserSession session)) {
            return false;
        }
        return id != null && id.equals(session.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
