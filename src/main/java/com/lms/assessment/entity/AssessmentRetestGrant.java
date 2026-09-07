package com.lms.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.FetchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks admin-granted extra attempts for a specific student on a specific assessment.
 *
 * <p>When an admin clicks "Retest", this row is created (or its {@code extraAttempts}
 * counter is incremented) instead of deleting the student's attempt history.
 * {@link com.lms.assessment.service.StudentAssessmentServiceImpl#startAttempt} checks
 * this table when the student has reached their default {@code maxAttempts} limit and
 * consumes one grant to allow an additional attempt.
 */
@Entity
@Table(
        name = "assessment_retest_grants",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_retest_grants_assessment_student",
                columnNames = {"assessment_id", "student_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentRetestGrant {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_retest_grants_assessment"))
    private Assessment assessment;

    /** The student who was granted the retest. */
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /**
     * How many additional attempts are available.
     * Each use of the grant decrements this by 1; the row is deleted when it reaches 0.
     */
    @Builder.Default
    @Column(name = "extra_attempts", nullable = false)
    private int extraAttempts = 1;

    /** The admin/instructor who granted the retest. May be null for system grants. */
    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;
}
