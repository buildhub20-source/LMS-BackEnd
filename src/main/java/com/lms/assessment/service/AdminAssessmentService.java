package com.lms.assessment.service;

import com.lms.assessment.dto.request.CreateAssessmentRequest;
import com.lms.assessment.dto.request.UpdateAssessmentRequest;
import com.lms.assessment.dto.response.AssessmentResponse;
import com.lms.assessment.dto.response.AssessmentSummaryResponse;
import com.lms.assessment.entity.AssessmentStatus;
import com.lms.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Admin-facing operations for assessment lifecycle management.
 */
public interface AdminAssessmentService {

    /** Create a new assessment in DRAFT status. */
    AssessmentResponse create(CreateAssessmentRequest request, UUID createdBy);

    /** Return a paginated list of all assessments, optionally filtered by status. */
    PageResponse<AssessmentSummaryResponse> list(AssessmentStatus status, Pageable pageable);

    /** Return the full detail of one assessment. */
    AssessmentResponse findById(UUID id);

    /**
     * Apply a partial update to a DRAFT assessment.
     * {@code totalMarks} is not accepted — it is auto-computed from question marks.
     */
    AssessmentResponse update(UUID id, UpdateAssessmentRequest request);

    /**
     * Delete a DRAFT assessment.
     * Only DRAFT assessments may be deleted; published ones must be CLOSED or ARCHIVED first.
     */
    void delete(UUID id);

    /**
     * Publish a DRAFT assessment after validating:
     * - title is set
     * - durationMinutes &gt; 0
     * - totalMarks &gt; 0 (auto-computed from questions)
     * - at least one question is attached
     * - every attached coding question has at least one test case
     */
    AssessmentResponse publish(UUID id);

    /**
     * Move a PUBLISHED assessment back to DRAFT.
     * Useful when the admin needs to make corrections after publishing.
     */
    AssessmentResponse unpublish(UUID id);

    /**
     * Close a PUBLISHED assessment — no new attempts will be accepted.
     * Existing in-progress attempts are still accessible to students.
     */
    AssessmentResponse close(UUID id);

    /**
     * Archive an assessment (any non-ARCHIVED status).
     * Archived assessments are hidden from all listing UIs.
     */
    AssessmentResponse archive(UUID id);

    /**
     * Get statistical analytics and student performance breakdowns for an assessment.
     */
    com.lms.assessment.dto.response.AssessmentAnalyticsResponse getAnalytics(UUID id);
}

