package com.lms.assessment.service;

import com.lms.assessment.dto.request.SaveSubmissionRequest;
import com.lms.assessment.dto.response.AssessmentSummaryResponse;
import com.lms.assessment.dto.response.AttemptDetailResponse;
import com.lms.assessment.dto.response.StartAttemptResponse;
import com.lms.assessment.dto.response.SubmissionResponse;
import com.lms.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StudentAssessmentService {

    /** List all published assessments. */
    PageResponse<AssessmentSummaryResponse> listPublished(Pageable pageable);

    /** Start a new attempt or return current IN_PROGRESS attempt. */
    StartAttemptResponse startAttempt(UUID assessmentId, UUID studentId);

    /** Save or update code draft for a question within an attempt. */
    SubmissionResponse saveSubmission(UUID attemptId, UUID studentId, SaveSubmissionRequest request);

    /** Finalize and submit an attempt. */
    AttemptDetailResponse submitAttempt(UUID attemptId, UUID studentId);

    /** Get current status and code submissions for an attempt. */
    AttemptDetailResponse getAttemptDetail(UUID attemptId, UUID studentId);
}
