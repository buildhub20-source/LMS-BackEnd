package com.lms.course.repository;

import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Data access for {@link Course} aggregates.
 *
 * <p>JpaSpecificationExecutor enables dynamic filtering (status, search, instructor)
 * without additional query methods.
 */
public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

    long countByStatus(CourseStatus status);
}
