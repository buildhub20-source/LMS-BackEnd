package com.lms.assessment.repository;

import com.lms.assessment.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

    List<AssessmentQuestion> findByAssessmentIdOrderByQuestionOrderAsc(UUID assessmentId);

    List<AssessmentQuestion> findBySectionIdOrderByQuestionOrderAsc(UUID sectionId);

    long countByAssessmentId(UUID assessmentId);

    boolean existsByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

    java.util.Optional<AssessmentQuestion> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

    /** All junction rows that reference a specific question (across all assessments). */
    List<AssessmentQuestion> findByQuestionId(UUID questionId);

    /**
     * Returns the IDs of all questions in an assessment that have at least one
     * test case, to support the publish validation.
     */
    @Query("""
            SELECT aq.question.id
            FROM AssessmentQuestion aq
            WHERE aq.assessment.id = :assessmentId
              AND EXISTS (
                  SELECT 1 FROM TestCase tc WHERE tc.question.id = aq.question.id
              )
            """)
    List<UUID> findQuestionIdsWithTestCases(@Param("assessmentId") UUID assessmentId);
}
