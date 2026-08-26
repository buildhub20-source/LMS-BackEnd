package com.lms.enrollment.service;

import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceAlreadyExistsException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import com.lms.course.repository.CourseRepository;
import com.lms.enrollment.dto.request.CreateEnrollmentRequest;
import com.lms.enrollment.dto.request.UpdateEnrollmentStatusRequest;
import com.lms.enrollment.dto.response.EnrollmentResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.enrollment.mapper.EnrollmentMapper;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentMapper enrollmentMapper;

    // --- Admin Operations ---

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getAllEnrollments(Pageable pageable) {
        return enrollmentRepository.findAll(pageable)
                .map(enrollmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(UUID id) {
        return enrollmentRepository.findById(id)
                .map(enrollmentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
    }

    @Override
    @Transactional
    public EnrollmentResponse createEnrollment(CreateEnrollmentRequest request) {
        return doCreateEnrollment(request, null); // Admin bypasses ownership check
    }

    @Override
    @Transactional
    public EnrollmentResponse updateEnrollmentStatus(UUID id, UpdateEnrollmentStatusRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
        return doUpdateEnrollmentStatus(enrollment, request);
    }

    // --- Instructor Operations ---

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getEnrollmentsByInstructor(UUID instructorId, Pageable pageable) {
        return enrollmentRepository.findByCourseInstructorId(instructorId, pageable)
                .map(enrollmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentByIdForInstructor(UUID id, UUID instructorId) {
        return enrollmentRepository.findByIdAndCourseInstructorId(id, instructorId)
                .map(enrollmentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found or access denied"));
    }

    @Override
    @Transactional
    public EnrollmentResponse createEnrollmentForInstructor(UUID instructorId, CreateEnrollmentRequest request) {
        return doCreateEnrollment(request, instructorId);
    }

    @Override
    @Transactional
    public EnrollmentResponse updateEnrollmentStatusForInstructor(UUID id, UUID instructorId, UpdateEnrollmentStatusRequest request) {
        Enrollment enrollment = enrollmentRepository.findByIdAndCourseInstructorId(id, instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found or access denied"));
        return doUpdateEnrollmentStatus(enrollment, request);
    }

    // --- Student Operations ---

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getEnrollmentsByStudent(UUID studentId, Pageable pageable) {
        return enrollmentRepository.findByStudentId(studentId, pageable)
                .map(enrollmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentByIdForStudent(UUID id, UUID studentId) {
        return enrollmentRepository.findByIdAndStudentId(id, studentId)
                .map(enrollmentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found or access denied"));
    }

    // --- Internal Helpers ---

    private EnrollmentResponse doCreateEnrollment(CreateEnrollmentRequest request, UUID requiredInstructorId) {
        if (enrollmentRepository.existsByStudentIdAndCourseId(request.studentId(), request.courseId())) {
            throw new ResourceAlreadyExistsException("Student is already enrolled in this course");
        }

        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.courseId()));

        if (requiredInstructorId != null && !requiredInstructorId.equals(course.getInstructorId())) {
            throw new BusinessRuleException("You do not have permission to enroll students in this course.");
        }

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BusinessRuleException("Cannot enroll in a course that is not PUBLISHED");
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        return enrollmentMapper.toResponse(saved);
    }

    private EnrollmentResponse doUpdateEnrollmentStatus(Enrollment enrollment, UpdateEnrollmentStatusRequest request) {
        // Prevent arbitrary changes
        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot modify a CANCELLED enrollment");
        }

        enrollment.setStatus(request.status());
        
        if (request.status() == EnrollmentStatus.COMPLETED && enrollment.getCompletedAt() == null) {
            enrollment.setCompletedAt(Instant.now());
        }

        Enrollment saved = enrollmentRepository.save(enrollment);
        return enrollmentMapper.toResponse(saved);
    }
}
