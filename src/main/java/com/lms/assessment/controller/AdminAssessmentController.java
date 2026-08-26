package com.lms.assessment.controller;

import com.lms.assessment.dto.request.CreateAssessmentRequest;
import com.lms.assessment.dto.request.UpdateAssessmentRequest;
import com.lms.assessment.dto.response.AssessmentAnalyticsResponse;
import com.lms.assessment.dto.response.AssessmentResponse;
import com.lms.assessment.dto.response.AssessmentSummaryResponse;
import com.lms.assessment.entity.AssessmentStatus;
import com.lms.assessment.service.AdminAssessmentService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin REST endpoints for assessment lifecycle management.
 *
 * <p>All endpoints require the {@code ASSESSMENT_CREATE} or {@code ASSESSMENT_VIEW}
 * permission as appropriate. studentId is NEVER accepted from the request body;
 * admin identity is always derived from the authenticated JWT principal.
 */
@Tag(name = "Admin — Assessments")
@RestController
@RequestMapping(ApiPaths.ADMIN_ASSESSMENTS)
@RequiredArgsConstructor
public class AdminAssessmentController {

    private final AdminAssessmentService assessmentService;

    // ---------------------------------------------------------------
    // POST /api/v1/admin/assessments
    // ---------------------------------------------------------------

    @Operation(summary = "Create a new assessment (saved as DRAFT)")
    @PostMapping
    @PreAuthorize("hasAuthority('ASSESSMENT_CREATE')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> create(
            @Valid @RequestBody CreateAssessmentRequest request) {

        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        AssessmentResponse response = assessmentService.create(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(response, "Assessment created successfully"));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/admin/assessments
    // ---------------------------------------------------------------

    @Operation(summary = "List all assessments (optionally filtered by status)")
    @GetMapping
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<AssessmentSummaryResponse>>> list(
            @RequestParam(required = false) AssessmentStatus status,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.of(assessmentService.list(status, pageable)));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/admin/assessments/{id}
    // ---------------------------------------------------------------

    @Operation(summary = "Get full details of one assessment")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(assessmentService.findById(id)));
    }

    // ---------------------------------------------------------------
    // PUT /api/v1/admin/assessments/{id}
    // ---------------------------------------------------------------

    @Operation(summary = "Update a DRAFT assessment")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssessmentRequest request) {

        return ResponseEntity.ok(
                ApiResponse.of(assessmentService.update(id, request), "Assessment updated"));
    }

    // ---------------------------------------------------------------
    // DELETE /api/v1/admin/assessments/{id}
    // ---------------------------------------------------------------

    @Operation(summary = "Delete a DRAFT assessment")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSESSMENT_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        assessmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Assessment deleted"));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/admin/assessments/{id}/publish
    // ---------------------------------------------------------------

    @Operation(summary = "Publish a DRAFT assessment (makes it visible to students)")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('ASSESSMENT_PUBLISH')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.of(assessmentService.publish(id), "Assessment published successfully"));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/admin/assessments/{id}/unpublish
    // ---------------------------------------------------------------

    @Operation(summary = "Move a PUBLISHED assessment back to DRAFT for further editing")
    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAuthority('ASSESSMENT_PUBLISH')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> unpublish(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.of(assessmentService.unpublish(id), "Assessment moved back to DRAFT"));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/admin/assessments/{id}/close
    // ---------------------------------------------------------------

    @Operation(summary = "Close a PUBLISHED assessment — stops accepting new attempts")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('ASSESSMENT_PUBLISH')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> close(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.of(assessmentService.close(id), "Assessment closed successfully"));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/admin/assessments/{id}/archive
    // ---------------------------------------------------------------

    @Operation(summary = "Archive an assessment — hides it from all listing UIs")
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('ASSESSMENT_DELETE')")
    public ResponseEntity<ApiResponse<AssessmentResponse>> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.of(assessmentService.archive(id), "Assessment archived successfully"));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/admin/assessments/{id}/analytics
    // ---------------------------------------------------------------

    @Operation(summary = "Get statistical analytics and individual student performance for an assessment")
    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<AssessmentAnalyticsResponse>> getAnalytics(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(assessmentService.getAnalytics(id)));
    }
}
