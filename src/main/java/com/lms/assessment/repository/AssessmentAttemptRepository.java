package com.lms.assessment.repository;

import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {

    List<AssessmentAttempt> findByAssessmentIdAndStudentIdOrderByStartedAtDesc(
            UUID assessmentId, UUID studentId);

    long countByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);

    /** Guards account deletion: attempts hold a RESTRICT key to users. */
    long countByStudentId(UUID studentId);

    Optional<AssessmentAttempt> findByIdAndStudentId(UUID id, UUID studentId);

    List<AssessmentAttempt> findByAssessmentIdOrderByStartedAtDesc(UUID assessmentId);

    List<AssessmentAttempt> findByStudentIdAndStatusOrderByStartedAtDesc(
            UUID studentId, AttemptStatus status);

    /** Fetch all attempts for a specific student on a specific assessment (used by retest). */
    List<AssessmentAttempt> findByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);

    /** Delete all attempts for a student on an assessment (used by retest, after clearing submissions). */
    void deleteByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);
}
