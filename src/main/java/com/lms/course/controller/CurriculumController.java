package com.lms.course.controller;

import com.lms.common.response.ApiResponse;
import com.lms.course.dto.request.CourseModuleRequest;
import com.lms.course.dto.request.GenerateUploadUrlRequest;
import com.lms.course.dto.request.LessonRequest;
import com.lms.course.dto.response.CourseAnalyticsResponse;
import com.lms.course.dto.response.CourseModuleResponse;
import com.lms.course.dto.response.GenerateUploadUrlResponse;
import com.lms.course.dto.response.LessonResponse;
import com.lms.course.service.CurriculumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST API for course curriculum management (modules and lessons).
 *
 * <p>All mutation endpoints require {@code COURSE_UPDATE} authority, which is
 * granted to instructors and administrators. Ownership enforcement (instructors
 * may only modify their own courses) is handled in {@link CurriculumService}.
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/curriculum")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService curriculumService;

    // ─── Modules ─────────────────────────────────────────────────────────────

    @PostMapping("/modules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity<ApiResponse<CourseModuleResponse>> addModule(
            @PathVariable UUID courseId,
            @RequestBody @Valid CourseModuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(curriculumService.addModule(courseId, request)));
    }

    @PutMapping("/modules/{moduleId}")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity<ApiResponse<CourseModuleResponse>> updateModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @RequestBody @Valid CourseModuleRequest request) {
        return ResponseEntity.ok(ApiResponse.of(curriculumService.updateModule(courseId, moduleId, request)));
    }

    @DeleteMapping("/modules/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity<Void> deleteModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId) {
        curriculumService.deleteModule(courseId, moduleId);
        return ResponseEntity.noContent().build();
    }

    // ─── Lessons ─────────────────────────────────────────────────────────────

    @PostMapping("/modules/{moduleId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity<ApiResponse<LessonResponse>> addLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @RequestBody @Valid LessonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(curriculumService.addLesson(courseId, moduleId, request)));
    }

    @PutMapping("/modules/{moduleId}/lessons/{lessonId}")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @RequestBody @Valid LessonRequest request) {
        return ResponseEntity.ok(ApiResponse.of(curriculumService.updateLesson(courseId, moduleId, lessonId, request)));
    }

    @DeleteMapping("/modules/{moduleId}/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId) {
        curriculumService.deleteLesson(courseId, moduleId, lessonId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/modules/{moduleId}/lessons/{lessonId}/thumbnail",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity<ApiResponse<LessonResponse>> uploadLessonThumbnail(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.of(
                curriculumService.uploadLessonThumbnail(courseId, moduleId, lessonId, file)));
    }

    // ─── Recordings ──────────────────────────────────────────────────────────

    @PostMapping("/modules/{moduleId}/lessons/{lessonId}/recording/upload-url")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity<ApiResponse<GenerateUploadUrlResponse>> generateUploadUrl(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @RequestBody @Valid GenerateUploadUrlRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                curriculumService.generateUploadUrl(courseId, moduleId, lessonId, request)));
    }

    @PostMapping(value = "/modules/{moduleId}/lessons/{lessonId}/recording/upload",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity<ApiResponse<GenerateUploadUrlResponse>> uploadRecording(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.of(
                curriculumService.uploadRecordingDirectly(courseId, moduleId, lessonId, file)));
    }

    @PostMapping("/modules/{moduleId}/lessons/{lessonId}/recording/{recordingId}/complete")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> completeRecordingUpload(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @PathVariable UUID recordingId) {
        curriculumService.completeRecordingUpload(courseId, moduleId, lessonId, recordingId);
        return ResponseEntity.ok(ApiResponse.message("Recording upload completed"));
    }

    // ─── Analytics ───────────────────────────────────────────────────────────

    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('COURSE_ANALYTICS_VIEW') or hasAuthority('COURSE_VIEW')")
    public ResponseEntity<ApiResponse<CourseAnalyticsResponse>> getCourseAnalytics(
            @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.of(curriculumService.getCourseAnalytics(courseId)));
    }
}
