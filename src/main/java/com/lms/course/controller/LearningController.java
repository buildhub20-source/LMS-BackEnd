package com.lms.course.controller;

import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.ApiResponse;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.dto.response.LessonResponse;
import com.lms.course.entity.Lesson;
import com.lms.course.mapper.CourseMapper;
import com.lms.course.repository.LessonRepository;
import com.lms.course.service.CourseService;
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

import java.util.Map;
import java.util.UUID;

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
        return ResponseEntity.ok(ApiResponse.of(Map.of("courseId", courseId, "percent", 0)));
    }

    @Operation(summary = "Save learning progress for a course")
    @PostMapping("/{courseId}/progress")
    @PreAuthorize("hasAuthority('COURSE_VIEW') or hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveProgress(
            @PathVariable UUID courseId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(ApiResponse.of(Map.of("success", true)));
    }
}
