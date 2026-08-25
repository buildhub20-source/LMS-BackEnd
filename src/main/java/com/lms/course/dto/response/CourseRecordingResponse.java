package com.lms.course.dto.response;

import com.lms.course.entity.RecordingStatus;

import java.time.Instant;
import java.util.UUID;

/** Public view of a course recording. */
public class CourseRecordingResponse {

    private UUID id;
    private UUID courseId;
    private String storageProvider;
    private String storageKey;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private Integer durationSeconds;
    private RecordingStatus status;
    private UUID createdBy;
    private String createdByName;
    private Instant createdAt;

    public CourseRecordingResponse() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }

    public String getStorageProvider() { return storageProvider; }
    public void setStorageProvider(String storageProvider) { this.storageProvider = storageProvider; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public RecordingStatus getStatus() { return status; }
    public void setStatus(RecordingStatus status) { this.status = status; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
