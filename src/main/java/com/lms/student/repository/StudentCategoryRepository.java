package com.lms.student.repository;

import com.lms.student.entity.StudentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentCategoryRepository extends JpaRepository<StudentCategory, UUID> {

    List<StudentCategory> findAllByOrderBySortOrderAsc();

    Optional<StudentCategory> findByNameIgnoreCase(String name);
}
