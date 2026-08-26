package com.lms.enrollment.controller;

import com.lms.enrollment.dto.request.CreateEnrollmentRequest;
import com.lms.enrollment.dto.request.UpdateEnrollmentStatusRequest;
import com.lms.enrollment.dto.response.EnrollmentResponse;
import com.lms.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/enrollments")
@RequiredArgsConstructor
public class AdminEnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('ENROLLMENT_VIEW') and hasRole('ADMIN')")
    public Page<EnrollmentResponse> getAllEnrollments(Pageable pageable) {
        return enrollmentService.getAllEnrollments(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ENROLLMENT_VIEW') and hasRole('ADMIN')")
    public EnrollmentResponse getEnrollmentById(@PathVariable UUID id) {
        return enrollmentService.getEnrollmentById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ENROLLMENT_CREATE') and hasRole('ADMIN')")
    public EnrollmentResponse createEnrollment(@Valid @RequestBody CreateEnrollmentRequest request) {
        return enrollmentService.createEnrollment(request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ENROLLMENT_UPDATE') and hasRole('ADMIN')")
    public EnrollmentResponse updateEnrollmentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEnrollmentStatusRequest request
    ) {
        return enrollmentService.updateEnrollmentStatus(id, request);
    }
}
