package com.lms.student.service;

import com.lms.common.response.PageResponse;
import com.lms.student.dto.request.CreateBatchRequest;
import com.lms.student.dto.request.UpdateBatchRequest;
import com.lms.student.dto.response.BatchResponse;
import com.lms.student.entity.BatchStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Batches: the dated cohorts learners are enrolled into. */
public interface BatchService {

    BatchResponse create(CreateBatchRequest request);

    BatchResponse findById(UUID id);

    PageResponse<BatchResponse> search(String search, BatchStatus status, Pageable pageable);

    BatchResponse update(UUID id, UpdateBatchRequest request);

    /** Refuses to delete a batch that still has learners in it. */
    void delete(UUID id);

    /** PLANNED and ONGOING batches, for the intake form's batch picker. */
    List<BatchResponse> findOpenForEnrolment();

    /** Batches assigned to one instructor, newest first. */
    List<BatchResponse> findByInstructor(UUID instructorUserId);
}
