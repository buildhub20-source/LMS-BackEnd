package com.lms.course.controller;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.service.StorageService;
import com.lms.course.entity.CourseRecording;
import com.lms.course.repository.CourseRecordingRepository;
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
    private final StorageService storageService;

    @GetMapping("/{recordingId}/stream")
    public ResponseEntity<InputStreamResource> streamRecording(@PathVariable UUID recordingId) {
        CourseRecording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Recording not found"));

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
}
