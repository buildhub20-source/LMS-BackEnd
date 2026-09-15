package com.lms.gamification.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

/** An admin-configurable badge that students can earn. */
@Entity
@Table(name = "gamification_badges")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Badge extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon", nullable = false, length = 100)
    private String icon;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "criteria_type", nullable = false, length = 50)
    private String criteriaType;

    @Column(name = "criteria_value", nullable = false)
    private Integer criteriaValue;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Badge badge)) return false;
        return id != null && id.equals(badge.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
