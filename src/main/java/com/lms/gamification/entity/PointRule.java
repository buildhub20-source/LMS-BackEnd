package com.lms.gamification.entity;

import com.lms.common.audit.Timestamped;
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

import java.util.Objects;
import java.util.UUID;

/** Configurable point value for a given event type. */
@Entity
@Table(name = "gamification_point_rules", uniqueConstraints = {
        @UniqueConstraint(name = "uk_point_rules_event_type", columnNames = "event_type")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PointRule extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PointRule pr)) return false;
        return id != null && id.equals(pr.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
