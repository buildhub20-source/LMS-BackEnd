package com.lms.assessment.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A Rubric defines structured evaluation criteria for subjective/manual question grading.
 */
@Entity
@Table(name = "rubrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rubric extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "rubric", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RubricCriterion> criteria = new ArrayList<>();

    public void addCriterion(RubricCriterion criterion) {
        criteria.add(criterion);
        criterion.setRubric(this);
    }

    public void removeCriterion(RubricCriterion criterion) {
        criteria.remove(criterion);
        criterion.setRubric(null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Rubric r)) return false;
        return id != null && id.equals(r.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
