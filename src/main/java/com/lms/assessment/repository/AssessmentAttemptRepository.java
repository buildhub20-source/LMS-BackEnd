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

    Optional<AssessmentAttempt> findByIdAndStudentId(UUID id, UUID studentId);

    List<AssessmentAttempt> findByAssessmentIdOrderByStartedAtDesc(UUID assessmentId);

    List<AssessmentAttempt> findByStudentIdAndStatusOrderByStartedAtDesc(
            UUID studentId, AttemptStatus status);

    long countByStudentId(UUID studentId);
}
