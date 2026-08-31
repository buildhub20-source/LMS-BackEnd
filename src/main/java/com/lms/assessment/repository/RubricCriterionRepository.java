package com.lms.assessment.repository;

import com.lms.assessment.entity.RubricCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, UUID> {
    List<RubricCriterion> findByRubricIdOrderByIdAsc(UUID rubricId);
}
