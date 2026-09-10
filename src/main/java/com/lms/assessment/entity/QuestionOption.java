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
 * An option for a {@link QuestionType#MULTIPLE_CHOICE} question.
 */
@Entity
@Table(name = "question_options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOption {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_question_options_question"))
    private Question question;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Builder.Default
    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof QuestionOption qo)) return false;
        return id != null && id.equals(qo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
