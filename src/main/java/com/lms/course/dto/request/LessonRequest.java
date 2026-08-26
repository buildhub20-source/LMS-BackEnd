package com.lms.course.dto.request;

import com.lms.course.entity.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class LessonRequest {

    @NotBlank(message = "Lesson title is required")
    @Size(max = 255, message = "Lesson title cannot exceed 255 characters")
    private String title;

    @NotNull(message = "Lesson type is required")
    private LessonType lessonType;

    private String content;
    private UUID recordingId;
    private Integer durationMinutes;
    private boolean freePreview;
    private String thumbnailUrl;
    private int sortOrder;

    public LessonRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LessonType getLessonType() { return lessonType; }
    public void setLessonType(LessonType lessonType) { this.lessonType = lessonType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public UUID getRecordingId() { return recordingId; }
    public void setRecordingId(UUID recordingId) { this.recordingId = recordingId; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public boolean isFreePreview() { return freePreview; }
    public void setFreePreview(boolean freePreview) { this.freePreview = freePreview; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
