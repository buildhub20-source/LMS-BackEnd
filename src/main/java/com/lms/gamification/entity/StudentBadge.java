package com.lms.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Records a badge award for a specific student. */
@Entity
@Table(name = "gamification_student_badges", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_badge", columnNames = {"student_id", "badge_id"})
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class StudentBadge {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Builder.Default
    @Column(name = "awarded_at", nullable = false)
    private Instant awardedAt = Instant.now();

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof StudentBadge sb)) return false;
        return id != null && id.equals(sb.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
