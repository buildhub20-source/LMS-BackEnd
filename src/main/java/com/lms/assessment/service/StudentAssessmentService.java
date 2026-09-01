package com.lms.assessment.service;

import com.lms.assessment.dto.request.SaveSubmissionRequest;
import com.lms.assessment.dto.response.AssessmentSummaryResponse;
import com.lms.assessment.dto.response.AttemptDetailResponse;
import com.lms.assessment.dto.response.StartAttemptResponse;
import com.lms.assessment.dto.response.SubmissionResponse;
import com.lms.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

import com.lms.assessment.dto.response.AssessmentResultReportResponse;
import com.lms.assessment.dto.response.AttemptHistoryResponse;
import java.util.List;

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

    /** Get history of all attempts taken by a student for an assessment. */
    List<AttemptHistoryResponse> getStudentAttemptHistory(UUID assessmentId, UUID studentId);

    /** Get comprehensive result report for a completed attempt. */
    AssessmentResultReportResponse getStudentResultReport(UUID attemptId, UUID studentId);

    /** Generate presigned Cloudflare R2 upload URL for screen recording. */
    com.lms.assessment.dto.response.GenerateAttemptRecordingUploadUrlResponse generateRecordingUploadUrl(
            UUID attemptId, UUID studentId, com.lms.assessment.dto.request.GenerateAttemptRecordingUploadUrlRequest request);

    /** Finalize Cloudflare R2 screen recording metadata on attempt. */
    void completeRecordingUpload(
            UUID attemptId, UUID studentId, com.lms.assessment.dto.request.CompleteAttemptRecordingUploadRequest request);

    /** Direct multipart upload fallback for screen recording file. */
    void uploadRecordingDirect(
            UUID attemptId, UUID studentId, org.springframework.web.multipart.MultipartFile file, Integer durationSeconds);

    /** Generate presigned playback URL for viewing attempt screen recording. */
    String getRecordingPlaybackUrl(UUID attemptId, UUID studentId);
}
