package com.lms.course.controller;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.response.ApiResponse;
import com.lms.common.service.StorageService;
import com.lms.course.dto.response.RecordingPlaybackUrlResponse;
import com.lms.course.entity.Course;
import com.lms.course.entity.CourseRecording;
import com.lms.course.entity.RecordingStatus;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.CourseRecordingRepository;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.security.authentication.AuthenticationService;
import com.lms.security.authentication.LmsUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller to stream video and document recordings directly from Cloudflare R2
 * for HTML5 player playback without requiring public bucket URLs or browser CORS setup.
 */
@RestController
@RequestMapping("/api/v1/recordings")
@RequiredArgsConstructor
public class RecordingStreamController {

    private final CourseRecordingRepository recordingRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StorageService storageService;

    @GetMapping("/{recordingId}/playback-url")
    public ResponseEntity<ApiResponse<RecordingPlaybackUrlResponse>> getPlaybackUrl(
            @PathVariable UUID recordingId) {
        CourseRecording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Recording not found"));
        authorizePlayback(recording);

        if (recording.getStatus() != RecordingStatus.READY) {
            throw new ApplicationException(ErrorCode.BUSINESS_RULE_VIOLATION, "Recording is not ready for playback");
        }

        String playbackUrl = storageService.generatePresignedGetUrl(recording.getStorageKey());
        if (playbackUrl == null) {
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR,
                    "Unable to generate a playback URL for this recording");
        }

        return ResponseEntity.ok(ApiResponse.of(new RecordingPlaybackUrlResponse(playbackUrl)));
    }

    @GetMapping("/{recordingId}/stream")
    public ResponseEntity<InputStreamResource> streamRecording(@PathVariable UUID recordingId) {
        CourseRecording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Recording not found"));
        authorizePlayback(recording);

        if (recording.getStatus() != RecordingStatus.READY) {
            throw new ApplicationException(ErrorCode.BUSINESS_RULE_VIOLATION, "Recording is not ready for playback");
        }

        var s3Stream = storageService.getObjectStream(recording.getStorageKey());
        if (s3Stream == null) {
            throw new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Recording content unavailable");
        }

        String mimeType = recording.getMimeType() != null ? recording.getMimeType() : "video/mp4";
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(mimeType);
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        if (recording.getFileSize() != null && recording.getFileSize() > 0) {
            headers.setContentLength(recording.getFileSize());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(s3Stream));
    }

    /**
     * A recording ID is not a capability.  Only platform administrators, the
     * course instructor/creator, or an actively enrolled learner may retrieve
     * the object (or receive a presigned URL for it).
     */
    private void authorizePlayback(CourseRecording recording) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        if (principal.getRoles().contains("ADMIN") || principal.getRoles().contains("SUPER_ADMIN")) {
            return;
        }

        Course course = courseRepository.findById(recording.getCourseId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found"));
        UUID userId = principal.getUserId();
        if (userId.equals(course.getInstructorId()) || userId.equals(course.getCreatedBy())) {
            return;
        }

        boolean activelyEnrolled = enrollmentRepository.findByStudentIdAndCourseId(userId, course.getId())
                .map(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .orElse(false);
        if (!activelyEnrolled) {
            throw new ApplicationException(ErrorCode.ACCESS_DENIED,
                    "You are not authorized to access this recording");
        }
    }
}
