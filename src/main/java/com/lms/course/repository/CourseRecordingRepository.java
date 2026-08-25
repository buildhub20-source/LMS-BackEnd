package com.lms.course.repository;

import com.lms.course.entity.CourseRecording;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Data access for {@link CourseRecording} metadata. */
public interface CourseRecordingRepository extends JpaRepository<CourseRecording, UUID> {

    List<CourseRecording> findByCourseIdOrderByCreatedAtDesc(UUID courseId);
}
