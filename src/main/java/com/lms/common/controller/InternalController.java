package com.lms.common.controller;

import com.lms.common.dto.internal.InternalCourseDto;
import com.lms.common.dto.internal.InternalEnrollmentDto;
import com.lms.common.dto.internal.InternalOrgSettingsDto;
import com.lms.common.dto.internal.InternalUserDto;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.course.repository.CourseRepository;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.organization.repository.OrganizationSettingsRepository;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only internal API consumed exclusively by trusted peer services
 * (e.g. lms-certificate-service).
 *
 * <p>Every request to {@code /api/v1/internal/**} is intercepted by
 * {@link com.lms.security.authorization.ServiceKeyAuthFilter} before it reaches
 * this controller. The filter requires a valid {@code X-Service-Key} header;
 * requests without it never reach here.
 *
 * <p>Only the minimal fields required by the cert service are projected.
 * No write operations are exposed.
 */
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OrganizationSettingsRepository orgSettingsRepository;

    /**
     * Returns minimal user info (name + email) by user ID.
     * Used by the cert service to personalise the generated PDF.
     */
    @GetMapping("/users/{id}")
    public InternalUserDto getUser(@PathVariable UUID id) {
        return userRepository.findById(id)
                .map(u -> new InternalUserDto(u.getId(), u.getName(), u.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    /**
     * Returns minimal course info (title, duration, thumbnail) by course ID.
     * Used by the cert service to populate the certificate body.
     */
    @GetMapping("/courses/{id}")
    public InternalCourseDto getCourse(@PathVariable UUID id) {
        return courseRepository.findById(id)
                .map(c -> new InternalCourseDto(
                        c.getId(),
                        c.getTitle(),
                        c.getDescription(),
                        c.getDurationMinutes(),
                        c.getThumbnailKey()))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }

    /**
     * Returns the enrollment record for a given student + course pair.
     * The cert service uses {@code completedAt} to populate the issue date on
     * the certificate.
     *
     * @param studentId the student's user ID
     * @param courseId  the course ID
     */
    @GetMapping("/enrollments")
    public InternalEnrollmentDto getEnrollment(@RequestParam UUID studentId,
                                                @RequestParam UUID courseId) {
        return enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .map(e -> new InternalEnrollmentDto(
                        e.getId(),
                        e.getStudent().getId(),
                        e.getCourse().getId(),
                        e.getStatus().name(),
                        e.getEnrolledAt(),
                        e.getCompletedAt()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found for student " + studentId + " in course " + courseId));
    }

    /**
     * Returns platform-wide branding settings used by the cert service to
     * apply the default certificate template (logo, colours, org name).
     */
    @GetMapping("/org-settings")
    public InternalOrgSettingsDto getOrgSettings() {
        return orgSettingsRepository.findFirstBy()
                .map(o -> new InternalOrgSettingsDto(
                        o.getName(),
                        o.getDomain(),
                        o.getLogoUrl(),
                        o.getPrimaryColor(),
                        o.getSupportEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("Organisation settings have not been configured"));
    }
}
