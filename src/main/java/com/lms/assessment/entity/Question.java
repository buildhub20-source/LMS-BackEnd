package com.lms.assessment.entity;

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

import java.util.Objects;
import java.util.UUID;

/**
 * A single problem/question that can be added to one or more assessments.
 *
 * <p>Questions are independent of assessments; the join is managed via
 * {@link AssessmentQuestion}. This allows the same question to be reused
 * across multiple assessments.
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "input_format", columnDefinition = "TEXT")
    private String inputFormat;

    @Column(name = "output_format", columnDefinition = "TEXT")
    private String outputFormat;

    @Column(name = "constraints", columnDefinition = "TEXT")
    private String constraints;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType = QuestionType.CODING;

    @Builder.Default
    @Column(name = "marks", nullable = false)
    private int marks = 10;

    /** Time limit for code execution (used by the judge in a later phase). */
    @Builder.Default
    @Column(name = "time_limit_ms", nullable = false)
    private int timeLimitMs = 2000;

    /** Memory limit for code execution (used by the judge in a later phase). */
    @Builder.Default
    @Column(name = "memory_limit_mb", nullable = false)
    private int memoryLimitMb = 256;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Question q)) return false;
        return id != null && id.equals(q.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
