package com.lms.assessment.repository;

import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    Page<Assessment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Assessment> findByStatusOrderByCreatedAtDesc(AssessmentStatus status, Pageable pageable);

    /** Serializes attempt creation so concurrent requests cannot exceed maxAttempts. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assessment from Assessment assessment where assessment.id = :id")
    Optional<Assessment> findByIdForAttemptStart(@Param("id") UUID id);

    long countByCreatedBy(UUID createdBy);

    long countByStatus(AssessmentStatus status);
}
