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
}
