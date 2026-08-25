package com.lms.course.repository;

import com.lms.course.entity.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {
}
