package com.lms.assessment.repository;

import com.lms.assessment.entity.AssessmentRetestGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentRetestGrantRepository extends JpaRepository<AssessmentRetestGrant, UUID> {

    /** Find the grant row for a specific student on a specific assessment. */
    Optional<AssessmentRetestGrant> findByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);
}
