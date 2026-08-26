package com.lms.student.repository;

import com.lms.student.entity.Batch;
import com.lms.student.entity.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID>, JpaSpecificationExecutor<Batch> {

    boolean existsByCodeIgnoreCase(String code);

    List<Batch> findByStatusInOrderByStartDateDesc(List<BatchStatus> statuses);

    List<Batch> findAllByOrderByStartDateDesc();

    List<Batch> findByInstructorIdOrderByStartDateDesc(UUID instructorId);
}
