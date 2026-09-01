package com.lms.assessment.controller;

import com.lms.assessment.dto.request.GradeSubmissionRequest;
import com.lms.assessment.dto.response.AttemptDetailResponse;
import com.lms.assessment.dto.response.SubmissionResponse;
import com.lms.assessment.service.GradingService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin — Assessment Grading Workflow")
@RestController
@RequestMapping(ApiPaths.ADMIN_ASSESSMENTS + "/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;

    @Operation(summary = "List pending submissions requiring manual evaluation")
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ASSESSMENT_UPDATE') or hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<PageResponse<SubmissionResponse>>> getPendingSubmissions(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(gradingService.getPendingSubmissions(pageable)));
    }

    @Operation(summary = "Grade a student submission with rubric evaluation and feedback")
    @PostMapping("/attempts/{attemptId}/grade")
    @PreAuthorize("hasAuthority('ASSESSMENT_UPDATE') or hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<AttemptDetailResponse>> gradeSubmission(
            @PathVariable UUID attemptId,
            @Valid @RequestBody GradeSubmissionRequest request) {

        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        AttemptDetailResponse response = gradingService.gradeSubmission(attemptId, request, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.of(response, "Submission graded successfully"));
    }
}
