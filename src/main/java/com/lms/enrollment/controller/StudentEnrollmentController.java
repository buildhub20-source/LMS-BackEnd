package com.lms.enrollment.controller;

import com.lms.enrollment.dto.response.EnrollmentResponse;
import com.lms.enrollment.service.EnrollmentService;
import com.lms.security.authentication.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/enrollments")
@RequiredArgsConstructor
public class StudentEnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public Page<EnrollmentResponse> getMyEnrollments(Pageable pageable) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return enrollmentService.getEnrollmentsByStudent(studentId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public EnrollmentResponse getEnrollmentById(@PathVariable UUID id) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return enrollmentService.getEnrollmentByIdForStudent(id, studentId);
    }
}
