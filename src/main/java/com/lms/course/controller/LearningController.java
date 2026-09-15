package com.lms.course.controller;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.ApiResponse;
import com.lms.course.dto.request.SaveLearningProgressRequest;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.dto.response.LessonResponse;
import com.lms.course.mapper.CourseMapper;
import com.lms.course.repository.LessonRepository;
import com.lms.course.service.CourseService;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.security.authentication.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/learning/courses")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('COURSE_VIEW') or hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
@Transactional(readOnly = true)
public class LearningController {
    private final CourseService courseService;
    private final LessonRepository lessonRepository;
    private final CourseMapper courseMapper;
    private final EnrollmentRepository enrollmentRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.of(courseService.findById(courseId)));
    }

    @GetMapping("/{courseId}/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<LessonResponse>> getLesson(@PathVariable UUID courseId,
                                                                 @PathVariable UUID lessonId) {
        courseService.findById(courseId);
        var lesson = lessonRepository.findById(lessonId)
                .filter(l -> l.getModule().getCourse().getId().equals(courseId))
                .orElseThrow(() -> ResourceNotFoundException.of("Lesson", lessonId));
        return ResponseEntity.ok(ApiResponse.of(courseMapper.toLessonResponse(lesson)));
    }

    @GetMapping("/{courseId}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgress(@PathVariable UUID courseId) {
        Enrollment enrollment = requireEnrollment(courseId);
        return ResponseEntity.ok(ApiResponse.of(progress(courseId, enrollment)));
    }

    @PostMapping("/{courseId}/progress")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveProgress(@PathVariable UUID courseId,
            @Valid @RequestBody SaveLearningProgressRequest request) {
        Enrollment enrollment = requireEnrollment(courseId);
        var allowed = lessonRepository.findByModuleCourseId(courseId).stream()
                .map(lesson -> lesson.getId()).collect(Collectors.toSet());
        if (!allowed.containsAll(request.completedLessonIds())) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED, "Completed lessons must belong to this course");
        }
        // Capture previously completed lessons for delta computation
        var previouslyCompleted = new HashSet<>(enrollment.getCompletedLessonIds());
        enrollment.getCompletedLessonIds().clear();
        enrollment.getCompletedLessonIds().addAll(request.completedLessonIds());
        Instant now = Instant.now();
        enrollment.setLastAccessedAt(now);
        if (enrollment.getStartedAt() == null) enrollment.setStartedAt(now);
        boolean complete = !allowed.isEmpty() && request.completedLessonIds().containsAll(allowed);
        boolean wasComplete = enrollment.getStatus() == EnrollmentStatus.COMPLETED;
        enrollment.setStatus(complete ? EnrollmentStatus.COMPLETED : EnrollmentStatus.ACTIVE);
        enrollment.setCompletedAt(complete ? (enrollment.getCompletedAt() == null ? now : enrollment.getCompletedAt()) : null);
        enrollmentRepository.save(enrollment);
        if (complete && !wasComplete) {
            eventPublisher.publishEvent(new com.lms.enrollment.event.EnrollmentCompletedEvent(
                    enrollment.getStudent().getId(), courseId,
                    com.lms.platform.runtime.TenantContext.current().map(tenant -> tenant.slug()).orElse(null)));
        }
        // Publish gamification events for newly completed lessons
        var newlyCompleted = new HashSet<>(request.completedLessonIds());
        newlyCompleted.removeAll(previouslyCompleted);
        UUID studentId = enrollment.getStudent().getId();
        for (UUID lessonId : newlyCompleted) {
            eventPublisher.publishEvent(new com.lms.gamification.event.LessonCompletedEvent(studentId, lessonId, courseId));
        }
        return ResponseEntity.ok(ApiResponse.of(progress(courseId, enrollment)));
    }

    private Enrollment requireEnrollment(UUID courseId) {
        courseService.findById(courseId);
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(
                AuthenticationService.requirePrincipal().getUserId(), courseId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ACCESS_DENIED, "You are not enrolled in this course"));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE && enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            throw new ApplicationException(ErrorCode.ACCESS_DENIED, "This enrollment is not active");
        }
        return enrollment;
    }

    private Map<String, Object> progress(UUID courseId, Enrollment enrollment) {
        var lessonIds = lessonRepository.findByModuleCourseId(courseId).stream()
                .map(lesson -> lesson.getId()).collect(Collectors.toSet());
        var completed = new HashSet<>(enrollment.getCompletedLessonIds());
        completed.retainAll(lessonIds);
        int percent = lessonIds.isEmpty() ? 0 : (int) Math.round(100.0 * completed.size() / lessonIds.size());
        return Map.of("courseId", courseId, "percent", percent, "completedLessonIds", completed);
    }
}
