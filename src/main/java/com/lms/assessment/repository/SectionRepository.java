package com.lms.assessment.repository;

import com.lms.assessment.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SectionRepository extends JpaRepository<Section, UUID> {

    List<Section> findByAssessmentIdOrderBySectionOrderAsc(UUID assessmentId);

    long countByAssessmentId(UUID assessmentId);
}
