package com.lms.analytics.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.course.repository.CourseRepository;
import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.assessment.entity.AssessmentStatus;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.invitation.repository.InvitationRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
    private final AssessmentRepository assessmentRepository;
    private final InvitationRepository invitationRepository;

    @Operation(summary = "Get admin system-wide analytics overview")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('AUDIT_VIEW')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminOverview() {
        long activeLearners = enrollmentRepository.countDistinctStudentsByStatus(EnrollmentStatus.ACTIVE);
        long totalCourses = courseRepository.count();
        long publishedCourses = courseRepository.countByStatus(CourseStatus.PUBLISHED);
        long totalEnrollments = enrollmentRepository.count();
        long completedEnrollments = enrollmentRepository.countByStatus(EnrollmentStatus.COMPLETED);
        long totalMembers = userRepository.count();
        long instructorCount = userRepository.countByRoleName("INSTRUCTOR");
        long assessmentCount = assessmentRepository.count();
        long publishedAssessments = assessmentRepository.countByStatus(AssessmentStatus.PUBLISHED);
        long pendingInvitations = invitationRepository.countByAcceptedAtIsNullAndExpiresAtAfter(Instant.now());

        Map<UUID, CourseInsight> engagementByCourse = new HashMap<>();
        for (Object[] row : enrollmentRepository.summarizeByCourse(EnrollmentStatus.COMPLETED)) {
            UUID courseId = (UUID) row[0];
            long enrollments = ((Number) row[1]).longValue();
            long completed = ((Number) row[2]).longValue();
            engagementByCourse.put(courseId, new CourseInsight(courseId, null, enrollments, completed));
        }

        List<CourseInsight> publishedCourseInsights = courseRepository.findAll().stream()
                .filter(course -> course.getStatus() == CourseStatus.PUBLISHED)
                .map(course -> withTitle(course, engagementByCourse.get(course.getId())))
                .toList();
        List<Map<String, Object>> topCourses = publishedCourseInsights.stream()
                .sorted(Comparator.comparingLong(CourseInsight::enrollments).reversed())
                .limit(5)
                .map(AnalyticsController::courseInsightMap)
                .toList();
        List<Map<String, Object>> atRiskCourses = publishedCourseInsights.stream()
                .filter(course -> course.enrollments() == 0 || course.completionRate() < 50)
                .sorted(Comparator.comparingInt(CourseInsight::completionRate))
                .limit(5)
                .map(AnalyticsController::courseInsightMap)
                .toList();

        List<Map<String, Object>> actionQueue = new ArrayList<>();
        if (pendingInvitations > 0) {
            actionQueue.add(Map.of("type", "invitations", "count", pendingInvitations,
                    "title", "Pending invitations", "detail", "Invitees still need to accept access."));
        }
        long unpublishedContent = totalCourses - publishedCourses;
        if (unpublishedContent > 0) {
            actionQueue.add(Map.of("type", "courses", "count", unpublishedContent,
                    "title", "Courses need publishing", "detail", "Draft, review, unpublished, or archived courses need attention."));
        }
        long unpublishedAssessments = assessmentCount - publishedAssessments;
        if (unpublishedAssessments > 0) {
            actionQueue.add(Map.of("type", "assessments", "count", unpublishedAssessments,
                    "title", "Assessments need publishing", "detail", "Make assessments available when they are ready."));
        }
        if (!atRiskCourses.isEmpty()) {
            actionQueue.add(Map.of("type", "risk", "count", atRiskCourses.size(),
                    "title", "Courses need a progress review", "detail", "Published courses have no enrollments or low completion."));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("activeLearners", activeLearners);
        data.put("totalCourses", totalCourses);
        data.put("publishedCourses", publishedCourses);
        data.put("totalEnrollments", totalEnrollments);
        data.put("completionRate", percentage(completedEnrollments, totalEnrollments));
        data.put("totalMembers", totalMembers);
        data.put("instructorCount", instructorCount);
        data.put("assessmentCount", assessmentCount);
        data.put("publishedAssessments", publishedAssessments);
        data.put("pendingInvitations", pendingInvitations);
        data.put("actionQueue", actionQueue);
        data.put("topCourses", topCourses);
        data.put("atRiskCourses", atRiskCourses);
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

    private static CourseInsight withTitle(Course course, CourseInsight engagement) {
        if (engagement == null) {
            return new CourseInsight(course.getId(), course.getTitle(), 0, 0);
        }
        return new CourseInsight(course.getId(), course.getTitle(), engagement.enrollments(), engagement.completed());
    }

    private static Map<String, Object> courseInsightMap(CourseInsight course) {
        return Map.of(
                "id", course.id(),
                "title", course.title(),
                "enrollments", course.enrollments(),
                "completed", course.completed(),
                "completionRate", course.completionRate());
    }

    private record CourseInsight(UUID id, String title, long enrollments, long completed) {
        private int completionRate() {
            return percentage(completed, enrollments);
        }
    }
}
