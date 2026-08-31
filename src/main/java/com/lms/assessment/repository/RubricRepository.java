package com.lms.assessment.repository;

import com.lms.assessment.entity.Rubric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RubricRepository extends JpaRepository<Rubric, UUID> {
    Page<Rubric> findByOrderByCreatedAtDesc(Pageable pageable);
}
