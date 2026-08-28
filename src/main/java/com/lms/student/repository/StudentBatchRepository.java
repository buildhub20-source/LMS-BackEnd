package com.lms.student.repository;

import com.lms.student.entity.StudentBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentBatchRepository extends JpaRepository<StudentBatch, UUID> {

    long countByBatchId(UUID batchId);

    boolean existsByBatchId(UUID batchId);
}
