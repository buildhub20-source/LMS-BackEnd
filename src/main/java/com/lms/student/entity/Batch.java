package com.lms.student.entity;

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

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A dated cohort running one course.
 *
 * <p>This is the grouping unit for a training centre, in place of the class and
 * section a school would use: batches start and finish on real dates, and a
 * learner can sit in more than one at a time.
 *
 * <p>{@code courseId} and {@code instructorId} are plain UUIDs rather than
 * associations, matching how {@link com.lms.course.entity.Course} refers to its
 * own creator and instructor.
 */
@Entity
@Table(name = "batches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Batch extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Null while a batch is scheduled before its course exists. */
    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "instructor_id")
    private UUID instructorId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** Free text, e.g. "Mon-Fri 10:00-13:00". */
    @Column(name = "schedule", length = 150)
    private String schedule;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    private DeliveryMode deliveryMode = DeliveryMode.OFFLINE;

    @Column(name = "capacity")
    private Integer capacity;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchStatus status = BatchStatus.PLANNED;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Batch batch)) {
            return false;
        }
        return id != null && id.equals(batch.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
