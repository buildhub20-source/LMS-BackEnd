package com.lms.gamification.entity;

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

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Records that a student performed learning activity on a given calendar date. */
@Entity
@Table(name = "gamification_streak_activities", uniqueConstraints = {
        @UniqueConstraint(name = "uk_streak_activity_student_date", columnNames = {"student_id", "activity_date"})
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class StreakActivity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof StreakActivity sa)) return false;
        return id != null && id.equals(sa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
