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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An evaluation score assigned by an evaluator for a single rubric criterion on a submission.
 */
@Entity
@Table(name = "rubric_scores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricScore {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_rubric_scores_attempt"))
    private AssessmentAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_rubric_scores_submission"))
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_rubric_scores_criterion"))
    private RubricCriterion criterion;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "evaluator_id", nullable = false, updatable = false)
    private UUID evaluatorId;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RubricScore rs)) return false;
        return id != null && id.equals(rs.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
