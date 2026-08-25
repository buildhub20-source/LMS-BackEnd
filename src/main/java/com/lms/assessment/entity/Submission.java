package com.lms.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The student's code submission for one question within an attempt.
 *
 * <p>One row per (attempt, question) pair — enforced by a unique constraint.
 * During the attempt the row acts as a draft: {@code status = DRAFT} and
 * {@code sourceCode} is updated on autosave. On final submission the row is
 * moved to {@code status = SUBMITTED}.
 *
 * <p>The {@code status} column is a plain VARCHAR so the judge system (Phase 2+)
 * can write its own values (ACCEPTED, WRONG_ANSWER, …) without a schema change.
 */
@Entity
@Table(
    name = "submissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_submissions_attempt_question",
        columnNames = {"attempt_id", "question_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_submissions_attempt"))
    private AssessmentAttempt attempt;

    /**
     * Raw UUID reference to avoid loading the Question aggregate on every
     * submission query.
     */
    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    /**
     * Denormalised for fast "all my submissions" queries without joining
     * through the attempt.
     */
    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Builder.Default
    @Column(name = "language", nullable = false, length = 50)
    private String language = "JAVA";

    @Column(name = "source_code", columnDefinition = "TEXT")
    private String sourceCode;

    /**
     * Open-ended status string.
     * Known values: DRAFT, SUBMITTED, PENDING_JUDGE, ACCEPTED, WRONG_ANSWER,
     * COMPILATION_ERROR, RUNTIME_ERROR, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED.
     */
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Submission s)) return false;
        return id != null && id.equals(s.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
