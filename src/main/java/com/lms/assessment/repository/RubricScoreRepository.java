package com.lms.assessment.repository;

import com.lms.assessment.entity.RubricScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RubricScoreRepository extends JpaRepository<RubricScore, UUID> {
    List<RubricScore> findByAttemptId(UUID attemptId);
    List<RubricScore> findBySubmissionId(UUID submissionId);
    void deleteBySubmissionId(UUID submissionId);
}
