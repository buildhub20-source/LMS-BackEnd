package com.lms.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Records a single student's attempt at a published assessment.
 *
 * <p>The timer is server-authoritative: {@code startedAt} is set on the server
 * when the student clicks "Start" and {@code expiresAt = startedAt +
 * assessment.durationMinutes}. The client displays a countdown derived from
 * these server timestamps.
 *
 * <p>Status transitions:
 * <pre>
 *   IN_PROGRESS  ──[student submits]──▶  SUBMITTED
 *   IN_PROGRESS  ──[server detects expiry]──▶  EXPIRED
 * </pre>
 * Once a terminal status is reached, no further modifications are permitted.
 */
@Entity
@Table(name = "assessment_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentAttempt {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_assessment_attempts_assessment"))
    private Assessment assessment;

    /**
     * The student who owns this attempt.
     * Stored as a raw UUID to avoid loading the full User aggregate.
     */
    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    /** Populated after grading (Phase 2+). Null until scored. */
    @Column(name = "score")
    private Integer score;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    public boolean isTerminal() {
        return status == AttemptStatus.SUBMITTED || status == AttemptStatus.EXPIRED;
    }

    public boolean isExpiredByTime() {
        return Instant.now().isAfter(expiresAt);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AssessmentAttempt a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
