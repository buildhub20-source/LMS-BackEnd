package com.lms.course.dto.request;

import jakarta.validation.constraints.Size;

/** Payload for updating course metadata. All fields are optional. */
public class UpdateCourseRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @Size(max = 30, message = "Level must not exceed 30 characters")
    private String level;

    private Integer durationMinutes;

    public UpdateCourseRequest() {
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
}
