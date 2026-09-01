package com.lms.assessment.controller;

import com.lms.assessment.dto.request.SaveSubmissionRequest;
import com.lms.assessment.dto.response.AssessmentResultReportResponse;
import com.lms.assessment.dto.response.AssessmentSummaryResponse;
import com.lms.assessment.dto.response.AttemptDetailResponse;
import com.lms.assessment.dto.response.AttemptHistoryResponse;
import com.lms.assessment.dto.response.StartAttemptResponse;
import com.lms.assessment.dto.response.SubmissionResponse;
import com.lms.assessment.service.StudentAssessmentService;
import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.security.authentication.AuthenticationService;
import com.lms.security.authentication.LmsUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Student — Assessments & Attempts")
@RestController
@RequestMapping(ApiPaths.STUDENT_ASSESSMENTS)
@RequiredArgsConstructor
public class StudentAssessmentController {

    private final StudentAssessmentService studentAssessmentService;

    @Operation(summary = "List all published assessments available to student")
    @GetMapping
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<AssessmentSummaryResponse>>> listPublished(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(studentAssessmentService.listPublished(pageable)));
    }

    @Operation(summary = "Start an assessment attempt (starts server-authoritative timer)")
    @PostMapping("/{assessmentId}/start")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<StartAttemptResponse>> startAttempt(@PathVariable UUID assessmentId) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        StartAttemptResponse response = studentAssessmentService.startAttempt(assessmentId, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(response, "Assessment attempt started successfully"));
    }

    @Operation(summary = "Autosave code draft for a question within an active attempt")
    @PostMapping("/attempts/{attemptId}/submissions")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> saveSubmission(
            @PathVariable UUID attemptId,
            @Valid @RequestBody SaveSubmissionRequest request) {

        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        SubmissionResponse response = studentAssessmentService.saveSubmission(attemptId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.of(response, "Code draft saved"));
    }

    @Operation(summary = "Finalize and submit an assessment attempt")
    @PostMapping("/attempts/{attemptId}/submit")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<AttemptDetailResponse>> submitAttempt(@PathVariable UUID attemptId) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        AttemptDetailResponse response = studentAssessmentService.submitAttempt(attemptId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.of(response, "Assessment attempt submitted successfully"));
    }

    @Operation(summary = "Get attempt details and code submissions")
    @GetMapping("/attempts/{attemptId}")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<AttemptDetailResponse>> getAttemptDetail(@PathVariable UUID attemptId) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        AttemptDetailResponse response = studentAssessmentService.getAttemptDetail(attemptId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "Get attempt history for an assessment")
    @GetMapping("/{assessmentId}/attempts")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<List<AttemptHistoryResponse>>> getAttemptHistory(@PathVariable UUID assessmentId) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        List<AttemptHistoryResponse> response = studentAssessmentService.getStudentAttemptHistory(assessmentId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "Get detailed result report for an attempt")
    @GetMapping("/attempts/{attemptId}/report")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<AssessmentResultReportResponse>> getResultReport(@PathVariable UUID attemptId) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        AssessmentResultReportResponse response = studentAssessmentService.getStudentResultReport(attemptId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "Generate presigned Cloudflare R2 upload URL for screen recording")
    @PostMapping("/attempts/{attemptId}/recording/upload-url")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<com.lms.assessment.dto.response.GenerateAttemptRecordingUploadUrlResponse>> generateRecordingUploadUrl(
            @PathVariable UUID attemptId,
            @Valid @RequestBody com.lms.assessment.dto.request.GenerateAttemptRecordingUploadUrlRequest request) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        com.lms.assessment.dto.response.GenerateAttemptRecordingUploadUrlResponse response =
                studentAssessmentService.generateRecordingUploadUrl(attemptId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.of(response, "Presigned upload URL generated"));
    }

    @Operation(summary = "Finalize screen recording upload to Cloudflare R2")
    @PostMapping("/attempts/{attemptId}/recording/complete")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<Void>> completeRecordingUpload(
            @PathVariable UUID attemptId,
            @Valid @RequestBody com.lms.assessment.dto.request.CompleteAttemptRecordingUploadRequest request) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        studentAssessmentService.completeRecordingUpload(attemptId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.of(null, "Recording upload completed"));
    }

    @Operation(summary = "Direct multipart upload fallback for screen recording")
    @PostMapping(value = "/attempts/{attemptId}/recording/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<Void>> uploadRecordingDirect(
            @PathVariable UUID attemptId,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @org.springframework.web.bind.annotation.RequestParam(value = "durationSeconds", required = false) Integer durationSeconds) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        studentAssessmentService.uploadRecordingDirect(attemptId, principal.getUserId(), file, durationSeconds);
        return ResponseEntity.ok(ApiResponse.of(null, "Screen recording uploaded successfully"));
    }

    @Operation(summary = "Get presigned playback URL for attempt screen recording")
    @GetMapping("/attempts/{attemptId}/recording/playback-url")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<String>> getRecordingPlaybackUrl(@PathVariable UUID attemptId) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        String playbackUrl = studentAssessmentService.getRecordingPlaybackUrl(attemptId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.of(playbackUrl));
    }
}
