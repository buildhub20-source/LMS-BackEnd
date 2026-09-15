package com.lms.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable, append-only points transaction.
 *
 * <p>The {@code idempotencyKey} prevents the same event from awarding points
 * twice even under retries or duplicate event delivery.
 */
@Entity
@Table(name = "gamification_points_ledger", uniqueConstraints = {
        @UniqueConstraint(name = "uk_points_ledger_idempotency", columnNames = "idempotency_key")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PointsLedger {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PointsLedger pl)) return false;
        return id != null && id.equals(pl.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
