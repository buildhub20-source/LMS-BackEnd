package com.lms.assessment.entity;

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
 * An assessment is a timed, graded collection of coding questions.
 *
 * <p>Lifecycle: DRAFT → PUBLISHED (→ CLOSED → ARCHIVED).
 * Only PUBLISHED assessments are visible to students.
 * The {@code createdBy} field stores the UUID of the admin who created
 * the assessment; it is populated from the JWT principal, never from
 * the request body.
 */
@Entity
@Table(name = "assessments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assessment extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 60;

    @Builder.Default
    @Column(name = "total_marks", nullable = false)
    private int totalMarks = 0;

    @Builder.Default
    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AssessmentStatus status = AssessmentStatus.DRAFT;

    /** Optional window: if null, the assessment is open-ended once published. */
    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    /**
     * The admin who created this assessment.
     * Stored as a raw UUID rather than a FK-backed @ManyToOne to avoid
     * loading the User aggregate on every assessment query.
     */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    public boolean isDraft() {
        return AssessmentStatus.DRAFT == status;
    }

    public boolean isPublished() {
        return AssessmentStatus.PUBLISHED == status;
    }

    public void publish() {
        this.status = AssessmentStatus.PUBLISHED;
    }

    public void close() {
        this.status = AssessmentStatus.CLOSED;
    }

    public void archive() {
        this.status = AssessmentStatus.ARCHIVED;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Assessment a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
