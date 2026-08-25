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
 * A single test case that validates a {@link Question}'s expected output.
 *
 * <p>{@code isSample} test cases are visible to students during the test.
 * {@code isHidden} test cases are only used by the judge and are NEVER
 * returned to the student through any API.
 */
@Entity
@Table(name = "test_cases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_test_cases_question"))
    private Question question;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "expected_output", nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    /** If true, this test case is shown to the student as a sample. */
    @Builder.Default
    @Column(name = "is_sample", nullable = false)
    private boolean sample = false;

    /**
     * If true, the test case is used only by the judge and must not be
     * returned to students through any API response.
     */
    @Builder.Default
    @Column(name = "is_hidden", nullable = false)
    private boolean hidden = true;

    /**
     * Relative weight used by the judge when computing a partial score.
     * Phase 1 does not use this — it is reserved for the judge phase.
     */
    @Builder.Default
    @Column(name = "weight", nullable = false)
    private int weight = 1;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TestCase tc)) return false;
        return id != null && id.equals(tc.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
