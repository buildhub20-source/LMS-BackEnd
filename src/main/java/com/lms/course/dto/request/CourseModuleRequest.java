package com.lms.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CourseModuleRequest {

    @NotBlank(message = "Module title is required")
    @Size(max = 255, message = "Module title cannot exceed 255 characters")
    private String title;

    private int sortOrder;

    public CourseModuleRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
