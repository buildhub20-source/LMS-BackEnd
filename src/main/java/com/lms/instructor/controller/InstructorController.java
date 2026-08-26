package com.lms.instructor.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.instructor.dto.request.CreateInstructorRequest;
import com.lms.instructor.dto.request.UpdateInstructorRequest;
import com.lms.instructor.dto.response.InstructorReferenceDataResponse;
import com.lms.instructor.dto.response.InstructorResponse;
import com.lms.instructor.entity.EmploymentType;
import com.lms.instructor.service.InstructorService;
import com.lms.student.dto.request.GeneratePhotoUploadUrlRequest;
import com.lms.student.dto.response.StudentPhotoUploadUrlResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API for instructor records.
 *
 * <p>Mirrors the learner API: onboarding an instructor creates the account, so
 * this is the only way an INSTRUCTOR account comes into existence with its
 * teaching detail attached.
 */
@Tag(name = "Instructors")
@RestController
@RequestMapping(ApiPaths.INSTRUCTORS)
@RequiredArgsConstructor
public class InstructorController {

    private final InstructorService instructorService;

    @Operation(summary = "Dropdown options for the instructor intake form",
            description = "Genders, ID proof types and employment types in one call")
    @GetMapping("/reference-data")
    @PreAuthorize("hasAuthority('INSTRUCTOR_VIEW')")
    public ResponseEntity<ApiResponse<InstructorReferenceDataResponse>> referenceData() {
        return ResponseEntity.ok(ApiResponse.of(instructorService.referenceData()));
    }

    @Operation(summary = "List instructors (paginated + filtered)")
    @GetMapping
    @PreAuthorize("hasAuthority('INSTRUCTOR_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<InstructorResponse>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EmploymentType employmentType,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.of(
                instructorService.search(search, employmentType, pageable)));
    }

    @Operation(summary = "Get a single instructor by ID",
            description = "Includes the batches they are assigned to")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INSTRUCTOR_VIEW')")
    public ResponseEntity<ApiResponse<InstructorResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(instructorService.findById(id)));
    }

    @Operation(summary = "Onboard a new instructor",
            description = "Creates the instructor record and the account behind it, and emails onboarding details")
    @PostMapping
    @PreAuthorize("hasAuthority('INSTRUCTOR_CREATE')")
    public ResponseEntity<ApiResponse<InstructorResponse>> create(
            @Valid @RequestBody CreateInstructorRequest request) {

        InstructorResponse created = instructorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @Operation(summary = "Update an instructor record")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('INSTRUCTOR_UPDATE')")
    public ResponseEntity<ApiResponse<InstructorResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInstructorRequest request) {

        return ResponseEntity.ok(ApiResponse.of(instructorService.update(id, request)));
    }

    @Operation(summary = "Remove an instructor record and its account")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INSTRUCTOR_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        instructorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Pre-signed URL for an instructor photo",
            description = "Upload the file to the returned URL, then submit the photoKey with the instructor payload")
    @PostMapping("/photo/upload-url")
    @PreAuthorize("hasAuthority('INSTRUCTOR_CREATE') or hasAuthority('INSTRUCTOR_UPDATE')")
    public ResponseEntity<ApiResponse<StudentPhotoUploadUrlResponse>> photoUploadUrl(
            @Valid @RequestBody GeneratePhotoUploadUrlRequest request) {

        return ResponseEntity.ok(ApiResponse.of(instructorService.generatePhotoUploadUrl(request)));
    }
}
