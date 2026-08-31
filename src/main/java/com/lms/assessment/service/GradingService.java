package com.lms.assessment.service;

import com.lms.assessment.dto.request.GradeSubmissionRequest;
import com.lms.assessment.dto.response.AttemptDetailResponse;
import com.lms.assessment.dto.response.SubmissionResponse;
import com.lms.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GradingService {
    PageResponse<SubmissionResponse> getPendingSubmissions(Pageable pageable);
    AttemptDetailResponse gradeSubmission(UUID attemptId, GradeSubmissionRequest request, UUID evaluatorId);
}
