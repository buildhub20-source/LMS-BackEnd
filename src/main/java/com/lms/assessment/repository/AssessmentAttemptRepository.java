package com.lms.assessment.repository;

import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {

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
}
