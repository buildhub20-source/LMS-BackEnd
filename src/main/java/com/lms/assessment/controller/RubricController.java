package com.lms.assessment.controller;

import com.lms.assessment.dto.request.CreateRubricRequest;
import com.lms.assessment.dto.response.RubricResponse;
import com.lms.assessment.service.RubricService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin — Assessment Rubrics")
@RestController
@RequestMapping(ApiPaths.ADMIN_ASSESSMENTS + "/rubrics")
@RequiredArgsConstructor
public class RubricController {

    private final RubricService rubricService;

    @Operation(summary = "Create a new grading rubric")
    @PostMapping
    @PreAuthorize("hasAuthority('ASSESSMENT_CREATE') or hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<RubricResponse>> createRubric(@Valid @RequestBody CreateRubricRequest request) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        RubricResponse response = rubricService.createRubric(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(response, "Rubric created successfully"));
    }

    @Operation(summary = "List all rubrics")
    @GetMapping
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW') or hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<PageResponse<RubricResponse>>> listRubrics(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(rubricService.listRubrics(pageable)));
    }

    @Operation(summary = "Get rubric by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW') or hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<RubricResponse>> getRubricById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(rubricService.getRubricById(id)));
    }

    @Operation(summary = "Delete rubric")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSESSMENT_DELETE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRubric(@PathVariable UUID id) {
        rubricService.deleteRubric(id);
        return ResponseEntity.ok(ApiResponse.message("Rubric deleted successfully"));
    }
}
