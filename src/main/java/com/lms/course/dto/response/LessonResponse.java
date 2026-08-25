package com.lms.course.dto.response;

import com.lms.course.entity.LessonType;

import java.util.UUID;

public class LessonResponse {
    private UUID id;
    private String title;
    private LessonType lessonType;
    private String content;
    private UUID recordingId;
    private Integer durationMinutes;
    private boolean freePreview;
    private int sortOrder;

    public LessonResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
