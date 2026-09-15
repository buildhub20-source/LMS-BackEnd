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

import java.util.Objects;
import java.util.UUID;

/** A configurable experience level based on total points. */
@Entity
@Table(name = "gamification_levels", uniqueConstraints = {
        @UniqueConstraint(name = "uk_levels_number", columnNames = "level_number")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class GamificationLevel extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "min_points", nullable = false)
    private Integer minPoints;

    /** Null for the highest level (no upper bound). */
    @Column(name = "max_points")
    private Integer maxPoints;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "color", length = 30)
    private String color;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GamificationLevel gl)) return false;
        return id != null && id.equals(gl.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
