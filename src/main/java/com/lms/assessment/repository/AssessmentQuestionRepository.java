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

    /** Returns one question count per assessment instead of issuing one count query per row. */
    @Query("""
            SELECT aq.assessment.id, COUNT(aq)
            FROM AssessmentQuestion aq
            WHERE aq.assessment.id IN :assessmentIds
            GROUP BY aq.assessment.id
            """)
    List<Object[]> countByAssessmentIds(@Param("assessmentIds") List<UUID> assessmentIds);

    boolean existsByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

    java.util.Optional<AssessmentQuestion> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

    /** All junction rows that reference a specific question (across all assessments). */
    List<AssessmentQuestion> findByQuestionId(UUID questionId);

    /**
     * Returns the IDs of all coding questions in an assessment that do NOT have
     * at least one test case.
     */
    @Query("""
            SELECT aq.question.id
            FROM AssessmentQuestion aq
            WHERE aq.assessment.id = :assessmentId
              AND aq.question.questionType = com.lms.assessment.entity.QuestionType.CODING
              AND NOT EXISTS (
                  SELECT 1 FROM TestCase tc WHERE tc.question.id = aq.question.id
              )
            """)
    List<UUID> findCodingQuestionIdsWithoutTestCases(@Param("assessmentId") UUID assessmentId);

    /**
     * Returns the IDs of all MCQ questions in an assessment that are invalid
     * (fewer than 2 options or 0 correct options).
     */
    @Query("""
            SELECT aq.question.id
            FROM AssessmentQuestion aq
            WHERE aq.assessment.id = :assessmentId
              AND aq.question.questionType = com.lms.assessment.entity.QuestionType.MULTIPLE_CHOICE
              AND (
                  (SELECT COUNT(qo) FROM QuestionOption qo WHERE qo.question.id = aq.question.id) < 2
                  OR
                  (SELECT COUNT(qo) FROM QuestionOption qo WHERE qo.question.id = aq.question.id AND qo.correct = true) < 1
              )
            """)
    List<UUID> findInvalidMcqQuestionIds(@Param("assessmentId") UUID assessmentId);

    /**
     * Returns the IDs of all questions in an assessment that have at least one
     * test case, to support legacy publish validation.
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
