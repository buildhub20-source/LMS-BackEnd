package com.lms.assessment.service;

import com.lms.assessment.dto.request.GradeSubmissionRequest;
import com.lms.assessment.dto.response.AttemptDetailResponse;
import com.lms.assessment.dto.response.SubmissionResponse;
import com.lms.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GradingService {
    PageResponse<SubmissionResponse> getPendingSubmissions(Pageable pageable);

    /** Grade an assessment owned by the evaluator. */
    default AttemptDetailResponse gradeSubmission(UUID attemptId, GradeSubmissionRequest request, UUID evaluatorId) {
        return gradeSubmission(attemptId, request, evaluatorId, false);
    }

    /**
     * Grades a submission. Tenant administrators may grade any assessment;
     * instructors and delegated evaluators remain limited to their own.
     */
    AttemptDetailResponse gradeSubmission(UUID attemptId, GradeSubmissionRequest request,
                                          UUID evaluatorId, boolean mayGradeAnyAssessment);
}
