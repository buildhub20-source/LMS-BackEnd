package com.lms.assessment.entity;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One AI grading suggestion for a coding answer. It is advice for the
 * instructor, never a score in its own right.
 */
@Entity
@Table(name = "ai_grading_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGradingResult {

    public static final String SUGGESTED = "SUGGESTED";
    public static final String FAILED = "FAILED";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "score", precision = 8, scale = 2)
    private BigDecimal score;

    @Column(name = "max_score")
    private Integer maxScore;

    @Column(name = "tests_passed")
    private Integer testsPassed;

    @Column(name = "tests_total")
    private Integer testsTotal;

    /** The AI service's full result: test results, rubric comments, feedback, suggestions. */
    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
