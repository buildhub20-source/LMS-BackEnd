package com.lms.student.repository;

import com.lms.student.entity.StudentBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StudentBatchRepository extends JpaRepository<StudentBatch, UUID> {

    long countByBatchId(UUID batchId);

    boolean existsByBatchId(UUID batchId);

    /** Returns the account IDs of learners enrolled in the supplied batch. */
    @Query("select distinct sb.studentProfile.user.id from StudentBatch sb where sb.batch.id = :batchId")
    List<UUID> findStudentUserIdsByBatchId(@Param("batchId") UUID batchId);
}
