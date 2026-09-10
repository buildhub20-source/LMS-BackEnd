package com.lms.analytics.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.course.repository.CourseRepository;
import com.lms.course.entity.CourseStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.security.authentication.AuthenticationService;
import com.lms.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Analytics endpoints providing dashboard overview metrics for admins, instructors, and students.
 */
@Tag(name = "Analytics")
@RestController
@RequestMapping(ApiPaths.ANALYTICS)
@RequiredArgsConstructor
public class AnalyticsController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Operation(summary = "Get admin system-wide analytics overview")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('AUDIT_VIEW')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminOverview() {
        long activeLearners = enrollmentRepository.countDistinctStudentsByStatus(EnrollmentStatus.ACTIVE);
        long publishedCourses = courseRepository.countByStatus(CourseStatus.PUBLISHED);
        long totalEnrollments = enrollmentRepository.count();
        long completedEnrollments = enrollmentRepository.countByStatus(EnrollmentStatus.COMPLETED);

        Map<String, Object> data = new HashMap<>();
        data.put("activeLearners", activeLearners);
        data.put("publishedCourses", publishedCourses);
        data.put("totalEnrollments", totalEnrollments);
        data.put("completionRate", percentage(completedEnrollments, totalEnrollments));
        data.put("recentActivity", java.util.List.of());

        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @Operation(summary = "Get instructor analytics overview")
    @GetMapping("/instructor")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInstructorOverview() {
        var principal = AuthenticationService.requirePrincipal();
        long totalEnrollments = enrollmentRepository.countByCourseInstructorId(principal.getUserId());
        long activeStudents = enrollmentRepository.countByCourseInstructorIdAndStatus(
                principal.getUserId(), EnrollmentStatus.ACTIVE);
        long completedStudents = enrollmentRepository.countByCourseInstructorIdAndStatus(
                principal.getUserId(), EnrollmentStatus.COMPLETED);
        long publishedCourses = courseRepository.countByInstructorId(principal.getUserId());

        Map<String, Object> data = new HashMap<>();
        data.put("totalStudents", activeStudents);
        data.put("activeCourses", publishedCourses);
        data.put("averageCompletion", percentage(completedStudents, totalEnrollments));

        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @Operation(summary = "Get student progress analytics")
    @GetMapping("/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStudentProgress() {
        long totalEnrollments = enrollmentRepository.countByStudentId(
                AuthenticationService.requirePrincipal().getUserId());
        long completedCourses = enrollmentRepository.countByStudentIdAndStatus(
                AuthenticationService.requirePrincipal().getUserId(), EnrollmentStatus.COMPLETED);

        Map<String, Object> data = new HashMap<>();
        data.put("enrolledCourses", totalEnrollments);
        data.put("completedCourses", completedCourses);
        data.put("overallProgress", percentage(completedCourses, totalEnrollments));

        return ResponseEntity.ok(ApiResponse.of(data));
    }

    private static int percentage(long numerator, long denominator) {
        return denominator == 0 ? 0 : (int) Math.round((numerator * 100.0) / denominator);
    }
}
