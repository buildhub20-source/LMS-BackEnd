package com.lms.course.controller;

import com.lms.common.response.ApiResponse;
import com.lms.course.dto.request.CourseModuleRequest;
import com.lms.course.dto.request.LessonRequest;
import com.lms.course.dto.response.CourseModuleResponse;
import com.lms.course.dto.response.LessonResponse;
import com.lms.course.service.CurriculumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/curriculum")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService curriculumService;

    @PostMapping("/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<CourseModuleResponse>> addModule(
            @PathVariable UUID courseId,
            @RequestBody @Valid CourseModuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(curriculumService.addModule(courseId, request)));
    }

    @PutMapping("/modules/{moduleId}")
    public ResponseEntity<ApiResponse<CourseModuleResponse>> updateModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @RequestBody @Valid CourseModuleRequest request) {
        return ResponseEntity.ok(ApiResponse.of(curriculumService.updateModule(courseId, moduleId, request)));
    }

    @DeleteMapping("/modules/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId) {
        curriculumService.deleteModule(courseId, moduleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/modules/{moduleId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<LessonResponse>> addLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @RequestBody @Valid LessonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(curriculumService.addLesson(courseId, moduleId, request)));
    }

    @PutMapping("/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @RequestBody @Valid LessonRequest request) {
        return ResponseEntity.ok(ApiResponse.of(curriculumService.updateLesson(courseId, moduleId, lessonId, request)));
    }

    @DeleteMapping("/modules/{moduleId}/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId) {
        curriculumService.deleteLesson(courseId, moduleId, lessonId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/modules/{moduleId}/lessons/{lessonId}/recording/upload-url")
    public ResponseEntity<ApiResponse<com.lms.course.dto.response.GenerateUploadUrlResponse>> generateUploadUrl(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @RequestBody @Valid com.lms.course.dto.request.GenerateUploadUrlRequest request) {
        return ResponseEntity.ok(ApiResponse.of(curriculumService.generateUploadUrl(courseId, moduleId, lessonId, request)));
    }

    @PostMapping(value = "/modules/{moduleId}/lessons/{lessonId}/recording/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<com.lms.course.dto.response.GenerateUploadUrlResponse>> uploadRecording(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.of(curriculumService.uploadRecordingDirectly(courseId, moduleId, lessonId, file)));
    }

    @org.springframework.web.bind.annotation.GetMapping("/analytics")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('COURSE_ANALYTICS_VIEW') or hasAuthority('COURSE_VIEW')")
    public ResponseEntity<ApiResponse<com.lms.course.dto.response.CourseAnalyticsResponse>> getCourseAnalytics(
            @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.of(curriculumService.getCourseAnalytics(courseId)));
    }

}
