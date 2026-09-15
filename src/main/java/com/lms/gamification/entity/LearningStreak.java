package com.lms.gamification.entity;

import com.lms.common.audit.Timestamped;
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

/** Per-student streak tracker. */
@Entity
@Table(name = "gamification_streaks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_streaks_student", columnNames = "student_id")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class LearningStreak extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Builder.Default
    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;

    @Builder.Default
    @Column(name = "longest_streak", nullable = false)
    private Integer longestStreak = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LearningStreak ls)) return false;
        return id != null && id.equals(ls.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
