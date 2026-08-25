package com.lms.course.mapper;

import com.lms.course.dto.response.CourseModuleResponse;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.entity.Course;
import com.lms.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** Maps {@link Course} entities to API response DTOs. */
@Component
public class CourseMapper {

    /**
     * Maps a course to a response, resolving creator and instructor names from the provided
     * user lookup map.
     *
     * @param course    the course entity
     * @param userNames a map of userId → displayName used to resolve names without extra queries
     */
    public CourseResponse toResponse(Course course, Map<UUID, String> userNames) {
        CourseResponse resp = new CourseResponse();
        resp.setId(course.getId());
        resp.setTitle(course.getTitle());
        resp.setDescription(course.getDescription());
        resp.setStatus(course.getStatus());
        resp.setLevel(course.getLevel());
        resp.setThumbnailKey(course.getThumbnailKey());
        resp.setDurationMinutes(course.getDurationMinutes());
        resp.setRejectionReason(course.getRejectionReason());
        resp.setCreatedBy(course.getCreatedBy());
        resp.setCreatedByName(userNames.getOrDefault(course.getCreatedBy(), "Unknown"));
        resp.setInstructorId(course.getInstructorId());
        if (course.getInstructorId() != null) {
            resp.setInstructorName(userNames.getOrDefault(course.getInstructorId(), "Unknown"));
        }
        resp.setCreatedAt(course.getCreatedAt());
        resp.setUpdatedAt(course.getUpdatedAt());
        resp.setPublishedAt(course.getPublishedAt());
        resp.setArchivedAt(course.getArchivedAt());

        if (course.getModules() != null) {
            resp.setModules(course.getModules().stream().map(this::toModuleResponse).toList());
        }

        return resp;
    }

    public CourseModuleResponse toModuleResponse(com.lms.course.entity.CourseModule module) {
        CourseModuleResponse mResp = new CourseModuleResponse();
        mResp.setId(module.getId());
        mResp.setTitle(module.getTitle());
        mResp.setSortOrder(module.getSortOrder());
        if (module.getLessons() != null) {
            mResp.setLessons(module.getLessons().stream().map(this::toLessonResponse).toList());
        }
        return mResp;
    }

    public com.lms.course.dto.response.LessonResponse toLessonResponse(com.lms.course.entity.Lesson lesson) {
        com.lms.course.dto.response.LessonResponse lResp = new com.lms.course.dto.response.LessonResponse();
        lResp.setId(lesson.getId());
        lResp.setTitle(lesson.getTitle());
        lResp.setLessonType(lesson.getLessonType());
        lResp.setContent(lesson.getContent());
        lResp.setRecordingId(lesson.getRecordingId());
        lResp.setDurationMinutes(lesson.getDurationMinutes());
        lResp.setFreePreview(lesson.isFreePreview());
        lResp.setThumbnailUrl(lesson.getThumbnailUrl());
        lResp.setSortOrder(lesson.getSortOrder());
        return lResp;
    }

    /** Convenience overload when user names are not needed (e.g. single-entity quick fetch). */
    public CourseResponse toResponse(Course course) {
        return toResponse(course, Map.of());
    }
}
