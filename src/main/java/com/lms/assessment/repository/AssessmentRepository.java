package com.lms.assessment.repository;

import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    Page<Assessment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Assessment> findByStatusOrderByCreatedAtDesc(AssessmentStatus status, Pageable pageable);

    long countByCreatedBy(UUID createdBy);
}
