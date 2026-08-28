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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

/**
 * Junction between {@link Assessment} and {@link Question}.
 *
 * <p>Carries ordering information and allows overriding a question's
 * default mark allocation at the assessment level.
 */
@Entity
@Table(
    name = "assessment_questions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_assessment_questions_pair",
        columnNames = {"assessment_id", "question_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentQuestion {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_assessment_questions_assessment"))
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_assessment_questions_question"))
    private Question question;

    @Builder.Default
    @Column(name = "question_order", nullable = false)
    private int questionOrder = 0;

    /** Mark allocation for this question in this assessment (may differ from question default). */
    @Builder.Default
    @Column(name = "marks", nullable = false)
    private int marks = 10;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", foreignKey = @ForeignKey(name = "fk_assessment_questions_section"))
    private Section section;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AssessmentQuestion aq)) return false;
        return id != null && id.equals(aq.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
