package com.lms.assessment.controller;

import com.lms.assessment.dto.request.CreateSectionRequest;
import com.lms.assessment.dto.request.UpdateSectionRequest;
import com.lms.assessment.dto.response.SectionResponse;
import com.lms.assessment.service.AdminSectionService;
import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin — Sections")
@RestController
@RequestMapping(ApiPaths.ADMIN_ASSESSMENTS)
@RequiredArgsConstructor
public class AdminSectionController {

    private final AdminSectionService sectionService;

    @Operation(summary = "Create a new section within an assessment")
    @PostMapping("/{assessmentId}/sections")
    @PreAuthorize("hasAuthority('ASSESSMENT_CREATE')")
    public ResponseEntity<ApiResponse<SectionResponse>> addSection(
            @PathVariable UUID assessmentId,
            @Valid @RequestBody CreateSectionRequest request) {

        SectionResponse response = sectionService.addSection(assessmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(response, "Section created successfully"));
    }

    @Operation(summary = "Get all sections for an assessment (with nested questions)")
    @GetMapping("/{assessmentId}/sections")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> getSections(
            @PathVariable UUID assessmentId) {

        return ResponseEntity.ok(
                ApiResponse.of(sectionService.getSectionsByAssessmentId(assessmentId)));
    }

    @Operation(summary = "Update a section's title or description")
    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
    public ResponseEntity<ApiResponse<SectionResponse>> updateSection(
            @PathVariable UUID sectionId,
            @Valid @RequestBody UpdateSectionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.of(sectionService.updateSection(sectionId, request), "Section updated"));
    }

    @Operation(summary = "Delete a section (questions are moved to unsectioned)")
    @DeleteMapping("/sections/{sectionId}")
    @PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> deleteSection(
            @PathVariable UUID sectionId) {

        sectionService.deleteSection(sectionId);
        return ResponseEntity.ok(ApiResponse.message("Section deleted"));
    }

    @Operation(summary = "Move a question into a section")
    @PutMapping("/sections/{sectionId}/questions/{assessmentQuestionId}")
    @PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> moveQuestionToSection(
            @PathVariable UUID sectionId,
            @PathVariable UUID assessmentQuestionId) {

        sectionService.moveQuestionToSection(assessmentQuestionId, sectionId);
        return ResponseEntity.ok(ApiResponse.message("Question moved to section"));
    }

    @Operation(summary = "Remove a question from its section (unsection it)")
    @PutMapping("/questions/{assessmentQuestionId}/unsection")
    @PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> unsectionQuestion(
            @PathVariable UUID assessmentQuestionId) {

        sectionService.moveQuestionToSection(assessmentQuestionId, null);
        return ResponseEntity.ok(ApiResponse.message("Question removed from section"));
    }
}
