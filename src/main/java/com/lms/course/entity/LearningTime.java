package com.lms.course.entity;

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
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Seconds a student spent on a course's lesson pages on one UTC day. */
@Entity
@Table(name = "learning_time", uniqueConstraints = @UniqueConstraint(
        name = "uk_learning_time_student_course_day", columnNames = {"student_id", "course_id", "study_date"}))
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class LearningTime {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "study_date", nullable = false)
    private LocalDate day;

    @Builder.Default
    @Column(name = "seconds", nullable = false)
    private int seconds = 0;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LearningTime t)) return false;
        return id != null && id.equals(t.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
