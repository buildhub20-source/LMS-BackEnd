package com.lms.enrollment.service;

import com.lms.enrollment.dto.request.CreateEnrollmentRequest;
import com.lms.enrollment.dto.request.UpdateEnrollmentStatusRequest;
import com.lms.enrollment.dto.response.EnrollmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EnrollmentService {

    // --- Admin Operations ---
    Page<EnrollmentResponse> getAllEnrollments(Pageable pageable);
    EnrollmentResponse getEnrollmentById(UUID id);
    EnrollmentResponse createEnrollment(CreateEnrollmentRequest request);
    EnrollmentResponse updateEnrollmentStatus(UUID id, UpdateEnrollmentStatusRequest request);

    // --- Instructor Operations ---
    Page<EnrollmentResponse> getEnrollmentsByInstructor(UUID instructorId, Pageable pageable);
    EnrollmentResponse getEnrollmentByIdForInstructor(UUID id, UUID instructorId);
    EnrollmentResponse createEnrollmentForInstructor(UUID instructorId, CreateEnrollmentRequest request);
    EnrollmentResponse updateEnrollmentStatusForInstructor(UUID id, UUID instructorId, UpdateEnrollmentStatusRequest request);

    // --- Student Operations ---
    Page<EnrollmentResponse> getEnrollmentsByStudent(UUID studentId, Pageable pageable);
    EnrollmentResponse getEnrollmentByIdForStudent(UUID id, UUID studentId);
}
