package com.lms.assessment.repository;

import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "assessment")
    List<AssessmentAttempt> findAllByOrderByStartedAtDesc();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "assessment")
    List<AssessmentAttempt> findByAssessment_CreatedByOrderByStartedAtDesc(UUID instructorId);

    List<AssessmentAttempt> findByAssessmentIdAndStudentIdOrderByStartedAtDesc(
            UUID assessmentId, UUID studentId);

    Page<AssessmentAttempt> findByAssessmentIdAndStudentIdOrderByStartedAtDesc(
            UUID assessmentId, UUID studentId, Pageable pageable);

    long countByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);

    /** Guards account deletion: attempts hold a RESTRICT key to users. */
    long countByStudentId(UUID studentId);

    Optional<AssessmentAttempt> findByIdAndStudentId(UUID id, UUID studentId);

    List<AssessmentAttempt> findByAssessmentIdOrderByStartedAtDesc(UUID assessmentId);

    List<AssessmentAttempt> findByStudentIdAndStatusOrderByStartedAtDesc(
            UUID studentId, AttemptStatus status);

    /** Average score across graded, submitted attempts on assessments an instructor created. */
    @Query("select avg(a.score) from AssessmentAttempt a "
            + "where a.assessment.createdBy = :instructorId and a.status = :status and a.score is not null")
    Double averageScoreOnAssessmentsCreatedBy(@Param("instructorId") UUID instructorId,
                                               @Param("status") AttemptStatus status);

    /** Submitted attempts on an instructor's assessments that still have no score. */
    long countByAssessment_CreatedByAndStatusAndScoreIsNull(UUID instructorId, AttemptStatus status);
}
