package com.lms.student.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.student.dto.request.StudentCategoryRequest;
import com.lms.student.dto.response.StudentCategoryResponse;
import com.lms.student.service.StudentCategoryService;
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

/**
 * Admission categories.
 *
 * <p>Guarded by the learner permissions rather than its own set: this is
 * reference data for the learner module, and a separate CATEGORY_* permission
 * would mean a migration for no real separation of duty.
 */
@Tag(name = "Student categories")
@RestController
@RequestMapping(ApiPaths.STUDENTS + "/categories")
@RequiredArgsConstructor
public class StudentCategoryController {

    private final StudentCategoryService categoryService;

    @Operation(summary = "List admission categories")
    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public ResponseEntity<ApiResponse<List<StudentCategoryResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.of(categoryService.findAll()));
    }

    @Operation(summary = "Create an admission category")
    @PostMapping
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    public ResponseEntity<ApiResponse<StudentCategoryResponse>> create(
            @Valid @RequestBody StudentCategoryRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(categoryService.create(request)));
    }

    @Operation(summary = "Rename an admission category")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    public ResponseEntity<ApiResponse<StudentCategoryResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody StudentCategoryRequest request) {

        return ResponseEntity.ok(ApiResponse.of(categoryService.update(id, request)));
    }

    @Operation(summary = "Delete an unused admission category")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
