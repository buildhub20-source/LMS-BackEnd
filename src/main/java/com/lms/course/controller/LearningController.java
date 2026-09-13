package com.lms.course.controller;

import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.ApiResponse;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.dto.response.LessonResponse;
import com.lms.course.entity.Lesson;
import com.lms.course.mapper.CourseMapper;
import com.lms.course.repository.LessonRepository;
import com.lms.course.service.CourseService;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.security.authentication.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API for student learning course player, lessons, and study progress tracking.
 */
@Tag(name = "Learning")
@RestController
@RequestMapping("/api/v1/learning/courses")
@RequiredArgsConstructor
public class LearningController {

    private final CourseService courseService;
    private final LessonRepository lessonRepository;
    private final CourseMapper courseMapper;
    private final EnrollmentRepository enrollmentRepository;

    private static final Map<String, ProgressData> PROGRESS_CACHE = new ConcurrentHashMap<>();

    private record ProgressData(int percent, List<String> completedLessonIds, Instant lastUpdated) {}

    @Operation(summary = "Get course details for learning player")
    @GetMapping("/{courseId}")
    @PreAuthorize("hasAuthority('COURSE_VIEW') or hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.of(courseService.findById(courseId)));
    }

    @Operation(summary = "Get a single lesson for learning player")
    @GetMapping("/{courseId}/lessons/{lessonId}")
    @PreAuthorize("hasAuthority('COURSE_VIEW') or hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> getLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> ResourceNotFoundException.of("Lesson", lessonId));
        return ResponseEntity.ok(ApiResponse.of(courseMapper.toLessonResponse(lesson)));
    }

    @Operation(summary = "Get learning progress for a course")
    @GetMapping("/{courseId}/progress")
    @PreAuthorize("hasAuthority('COURSE_VIEW') or hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgress(@PathVariable UUID courseId) {
        UUID userId = AuthenticationService.requirePrincipal().getUserId();
        String key = userId + ":" + courseId;
        ProgressData data = PROGRESS_CACHE.get(key);

        int percent = (data != null) ? data.percent() : 0;
        List<String> completedIds = (data != null) ? data.completedLessonIds() : Collections.emptyList();

        return ResponseEntity.ok(ApiResponse.of(Map.of(
                "courseId", courseId,
                "percent", percent,
                "completedLessonIds", completedIds
        )));
    }

    @Operation(summary = "Save learning progress for a course")
    @PostMapping("/{courseId}/progress")
    @PreAuthorize("hasAuthority('COURSE_VIEW') or hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveProgress(
            @PathVariable UUID courseId,
            @RequestBody(required = false) Map<String, Object> payload) {
        UUID userId = AuthenticationService.requirePrincipal().getUserId();
        String key = userId + ":" + courseId;

        int percent = 0;
        List<String> completedLessonIds = Collections.emptyList();

        if (payload != null) {
            if (payload.containsKey("percent") && payload.get("percent") instanceof Number num) {
                percent = Math.min(100, Math.max(0, num.intValue()));
            }
            if (payload.containsKey("completedLessonIds") && payload.get("completedLessonIds") instanceof List<?> list) {
                completedLessonIds = list.stream().map(Object::toString).toList();
            }
        }

        PROGRESS_CACHE.put(key, new ProgressData(percent, completedLessonIds, Instant.now()));

        // Update enrollment timestamps and status
        final int finalPercent = percent;
        enrollmentRepository.findByStudentIdAndCourseId(userId, courseId).ifPresent(enrollment -> {
            enrollment.setLastAccessedAt(Instant.now());
            if (enrollment.getStartedAt() == null) {
                enrollment.setStartedAt(Instant.now());
            }
            if (finalPercent >= 100 && enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
                enrollment.setStatus(EnrollmentStatus.COMPLETED);
                enrollment.setCompletedAt(Instant.now());
            }
            enrollmentRepository.save(enrollment);
        });

        return ResponseEntity.ok(ApiResponse.of(Map.of("success", true, "percent", percent)));
    }
}
