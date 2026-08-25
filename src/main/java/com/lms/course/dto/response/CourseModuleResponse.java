package com.lms.course.dto.response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CourseModuleResponse {
    private UUID id;
    private String title;
    private int sortOrder;
    private List<LessonResponse> lessons = new ArrayList<>();

    public CourseModuleResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public List<LessonResponse> getLessons() { return lessons; }
    public void setLessons(List<LessonResponse> lessons) { this.lessons = lessons; }
}
