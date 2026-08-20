package com.lms.invitation.entity;

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
 * An invitation to activate an administrator-created account.
 *
 * <p>The account row in {@code users} already exists when the invitation is
 * issued, with a null password and {@code is_active = false}. Accepting the
 * invitation sets the password and activates the account.
 *
 * <p>Only the SHA-256 digest of the token is stored. The raw token exists once,
 * in the invitation email.
 *
 * <p>The ERD has no status column, so status is derived from
 * {@code accepted_at} and {@code expires_at}.
 */
@Entity
@Table(name = "user_invitation",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_invitation_token_hash", columnNames = "token_hash"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invitation {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_invitation_user"))
    private User user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by",
            foreignKey = @ForeignKey(name = "fk_user_invitation_invited_by"))
    private User invitedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public InvitationStatus status(Instant now) {
        if (isAccepted()) {
            return InvitationStatus.ACCEPTED;
        }
        return isExpired(now) ? InvitationStatus.EXPIRED : InvitationStatus.PENDING;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Invitation invitation)) {
            return false;
        }
        return id != null && id.equals(invitation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
