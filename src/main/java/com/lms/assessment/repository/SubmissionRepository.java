package com.lms.assessment.repository;

import com.lms.assessment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByAttemptIdOrderByQuestionIdAsc(UUID attemptId);

    Optional<Submission> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(UUID studentId);

    Page<Submission> findByStatusOrderBySubmittedAtDesc(String status, Pageable pageable);

    /** Guards account deletion: submissions hold a RESTRICT key to users. */
    long countByStudentId(UUID studentId);
}
