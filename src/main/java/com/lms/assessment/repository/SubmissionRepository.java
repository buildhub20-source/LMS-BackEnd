package com.lms.assessment.repository;

import com.lms.assessment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByAttemptIdOrderByQuestionIdAsc(UUID attemptId);

    Optional<Submission> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(UUID studentId);

    long countByStudentId(UUID studentId);
}
