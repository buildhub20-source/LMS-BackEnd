package com.lms.student.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** One learner's enrolment in one batch. */
@Entity
@Table(name = "student_batches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentBatch extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Builder.Default
    @Column(name = "enrolled_on", nullable = false)
    private LocalDate enrolledOn = LocalDate.now();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrolmentStatus status = EnrolmentStatus.ACTIVE;

    @Column(name = "completed_on")
    private LocalDate completedOn;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StudentBatch enrolment)) {
            return false;
        }
        return id != null && id.equals(enrolment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
