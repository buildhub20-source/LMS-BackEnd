package com.lms.assessment.service;

import com.lms.assessment.dto.request.CreateSectionRequest;
import com.lms.assessment.dto.request.UpdateSectionRequest;
import com.lms.assessment.dto.response.SectionResponse;

import java.util.List;
import java.util.UUID;

/**
 * Admin operations for assessment sections.
 */
public interface AdminSectionService {

    SectionResponse addSection(UUID assessmentId, CreateSectionRequest request);

    List<SectionResponse> getSectionsByAssessmentId(UUID assessmentId);

    SectionResponse updateSection(UUID sectionId, UpdateSectionRequest request);

    void deleteSection(UUID sectionId);

    void moveQuestionToSection(UUID assessmentQuestionId, UUID sectionId);
}
