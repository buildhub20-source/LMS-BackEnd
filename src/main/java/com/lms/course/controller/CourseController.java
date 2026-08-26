package com.lms.course.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.course.dto.request.CreateCourseRequest;
import com.lms.course.dto.request.RejectCourseRequest;
import com.lms.course.dto.request.UpdateCourseRequest;
import com.lms.course.dto.response.CourseRecordingResponse;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.entity.CourseRecording;
import com.lms.course.entity.CourseStatus;
import com.lms.course.repository.CourseRecordingRepository;
import com.lms.course.service.CourseService;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API for course management.
 *
 * <p>All endpoints require authentication. Individual actions are further
 * guarded by permissions from the RBAC model (Phase 0).
 */
@Tag(name = "Courses")
@RestController
@RequestMapping(ApiPaths.COURSES)
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final com.lms.course.service.CurriculumService curriculumService;
    private final CourseRecordingRepository recordingRepository;
    private final UserRepository userRepository;

    @Operation(summary = "List courses (paginated + filtered)")
    @GetMapping
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CourseStatus status,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.of(courseService.search(search, status, pageable)));
    }

    @Operation(summary = "Get a single course by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public ResponseEntity<ApiResponse<CourseResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(courseService.findById(id)));
    }

    @Operation(summary = "Get course statistical analytics")
    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasAuthority('COURSE_ANALYTICS_VIEW') or hasAuthority('COURSE_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<com.lms.course.dto.response.CourseAnalyticsResponse>> getCourseAnalytics(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(curriculumService.getCourseAnalytics(id)));
    }

    @Operation(summary = "Create a new course")
    @PostMapping
    @PreAuthorize("hasAuthority('COURSE_CREATE')")
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @Valid @RequestBody CreateCourseRequest request) {

        CourseResponse created = courseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @Operation(summary = "Update course metadata")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity<ApiResponse<CourseResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCourseRequest request) {

        return ResponseEntity.ok(ApiResponse.of(courseService.update(id, request)));
    }

    @Operation(summary = "Delete a DRAFT course (hard delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Operation(summary = "Publish a course (Admin direct-publish from DRAFT or UNPUBLISHED)")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('COURSE_PUBLISH')")
    public ResponseEntity<ApiResponse<CourseResponse>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(courseService.publish(id),
                "Course published successfully"));
    }

    @Operation(summary = "Unpublish a published course")
    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAuthority('COURSE_UNPUBLISH')")
    public ResponseEntity<ApiResponse<CourseResponse>> unpublish(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(courseService.unpublish(id),
                "Course unpublished"));
    }

    @Operation(summary = "Archive a course (PUBLISHED or UNPUBLISHED → ARCHIVED)")
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('COURSE_ARCHIVE')")
    public ResponseEntity<ApiResponse<CourseResponse>> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(courseService.archive(id),
                "Course archived"));
    }

    @Operation(summary = "Submit a course for admin review (Instructor action)")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('COURSE_SUBMIT')")
    public ResponseEntity<ApiResponse<CourseResponse>> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(courseService.submit(id),
                "Course submitted for review"));
    }

    @Operation(summary = "Approve a submitted course (Admin)")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('COURSE_APPROVE')")
    public ResponseEntity<ApiResponse<CourseResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(courseService.approve(id),
                "Course approved and published"));
    }

    @Operation(summary = "Reject a submitted course (Admin)")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('COURSE_REJECT')")
    public ResponseEntity<ApiResponse<CourseResponse>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) RejectCourseRequest request) {

        return ResponseEntity.ok(ApiResponse.of(courseService.reject(id, request),
                "Course rejected"));
    }

    // ─── Recordings ────────────────────────────────────────────────────────

    @Operation(summary = "List recordings for a course")
    @GetMapping("/{id}/recordings")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public ResponseEntity<ApiResponse<List<CourseRecordingResponse>>> getRecordings(
            @PathVariable UUID id) {

        // Verify the course exists
        courseService.findById(id);

        List<CourseRecording> recordings = recordingRepository.findByCourseIdOrderByCreatedAtDesc(id);

        // Resolve creator names in a single batch
        var creatorIds = recordings.stream()
                .map(CourseRecording::getCreatedBy)
                .collect(Collectors.toSet());
        Map<UUID, String> names = creatorIds.isEmpty() ? Map.of()
                : userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        List<CourseRecordingResponse> response = recordings.stream().map(rec -> {
            CourseRecordingResponse dto = new CourseRecordingResponse();
            dto.setId(rec.getId());
            dto.setCourseId(rec.getCourseId());
            dto.setStorageProvider(rec.getStorageProvider());
            dto.setStorageKey(rec.getStorageKey());
            dto.setFileName(rec.getFileName());
            dto.setFileSize(rec.getFileSize());
            dto.setMimeType(rec.getMimeType());
            dto.setDurationSeconds(rec.getDurationSeconds());
            dto.setStatus(rec.getStatus());
            dto.setCreatedBy(rec.getCreatedBy());
            dto.setCreatedByName(names.getOrDefault(rec.getCreatedBy(), "Unknown"));
            dto.setCreatedAt(rec.getCreatedAt());
            return dto;
        }).toList();

        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
