package com.lms.assessment.repository;

import com.lms.assessment.entity.AiGradingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiGradingResultRepository extends JpaRepository<AiGradingResult, UUID> {

    Optional<AiGradingResult> findFirstBySubmissionIdOrderByCreatedAtDesc(UUID submissionId);
}
