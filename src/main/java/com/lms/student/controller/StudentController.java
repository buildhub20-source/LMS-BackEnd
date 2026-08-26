package com.lms.student.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.student.dto.request.CreateStudentRequest;
import com.lms.student.dto.request.GeneratePhotoUploadUrlRequest;
import com.lms.student.dto.request.UpdateStudentRequest;
import com.lms.student.dto.response.StudentPhotoUploadUrlResponse;
import com.lms.student.dto.response.StudentReferenceDataResponse;
import com.lms.student.dto.response.StudentResponse;
import com.lms.student.entity.EnrolmentStatus;
import com.lms.student.service.StudentService;
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
 * REST API for learner records.
 *
 * <p>There is no {@code POST /users} equivalent here either: admitting a
 * learner creates the account, so this is the only way a STUDENT account comes
 * into existence with its enrolment attached.
 */
@Tag(name = "Students")
@RestController
@RequestMapping(ApiPaths.STUDENTS)
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "Dropdown options for the learner intake form",
            description = "Open batches, categories, genders and ID proof types in one call")
    @GetMapping("/reference-data")
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public ResponseEntity<ApiResponse<StudentReferenceDataResponse>> referenceData() {
        return ResponseEntity.ok(ApiResponse.of(studentService.referenceData()));
    }

    @Operation(summary = "List learners (paginated + filtered)")
    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<StudentResponse>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) EnrolmentStatus enrolmentStatus,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.of(
                studentService.search(search, batchId, enrolmentStatus, pageable)));
    }

    @Operation(summary = "Get a single learner by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public ResponseEntity<ApiResponse<StudentResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(studentService.findById(id)));
    }

    @Operation(summary = "Admit a new learner",
            description = "Creates the learner record and the account behind it, and emails onboarding details")
    @PostMapping
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    public ResponseEntity<ApiResponse<StudentResponse>> create(
            @Valid @RequestBody CreateStudentRequest request) {

        StudentResponse created = studentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @Operation(summary = "Update a learner record")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    public ResponseEntity<ApiResponse<StudentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudentRequest request) {

        return ResponseEntity.ok(ApiResponse.of(studentService.update(id, request)));
    }

    @Operation(summary = "Remove a learner record and its account")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Pre-signed URL for a learner photo",
            description = "Upload the file to the returned URL, then submit the photoKey with the learner payload")
    @PostMapping("/photo/upload-url")
    @PreAuthorize("hasAuthority('STUDENT_CREATE') or hasAuthority('STUDENT_UPDATE')")
    public ResponseEntity<ApiResponse<StudentPhotoUploadUrlResponse>> photoUploadUrl(
            @Valid @RequestBody GeneratePhotoUploadUrlRequest request) {

        return ResponseEntity.ok(ApiResponse.of(studentService.generatePhotoUploadUrl(request)));
    }
}
