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

import java.util.Objects;
import java.util.UUID;

/**
 * An individual criterion within a Rubric (e.g. Code Structure, Edge Case Handling, Optimization).
 */
@Entity
@Table(name = "rubric_criteria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricCriterion {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubric_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_rubric_criteria_rubric"))
    private Rubric rubric;

    @Column(name = "criterion_name", nullable = false, length = 255)
    private String criterionName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "max_points", nullable = false)
    private int maxPoints = 10;

    @Builder.Default
    @Column(name = "weight", nullable = false)
    private double weight = 1.0;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RubricCriterion c)) return false;
        return id != null && id.equals(c.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
