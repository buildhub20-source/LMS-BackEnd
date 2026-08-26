package com.lms.student.mapper;

import com.lms.student.dto.response.BatchResponse;
import com.lms.student.entity.Batch;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** Maps {@link Batch} entities to API response DTOs. */
@Component
public class BatchMapper {

    /**
     * @param courseTitles   courseId → title, so a list of batches resolves names
     *                       without a query per row
     * @param userNames      userId → display name, same reason
     * @param enrolledCount  learners currently in this batch
     */
    public BatchResponse toResponse(Batch batch,
                                    Map<UUID, String> courseTitles,
                                    Map<UUID, String> userNames,
                                    long enrolledCount) {

        BatchResponse resp = new BatchResponse();
        resp.setId(batch.getId());
        resp.setCode(batch.getCode());
        resp.setName(batch.getName());

        resp.setCourseId(batch.getCourseId());
        if (batch.getCourseId() != null) {
            resp.setCourseTitle(courseTitles.get(batch.getCourseId()));
        }

        resp.setInstructorId(batch.getInstructorId());
        if (batch.getInstructorId() != null) {
            resp.setInstructorName(userNames.get(batch.getInstructorId()));
        }

        resp.setStartDate(batch.getStartDate());
        resp.setEndDate(batch.getEndDate());
        resp.setSchedule(batch.getSchedule());
        resp.setDeliveryMode(batch.getDeliveryMode());
        resp.setCapacity(batch.getCapacity());
        resp.setStatus(batch.getStatus());
        resp.setEnrolledCount(enrolledCount);

        resp.setCreatedAt(batch.getCreatedAt());
        resp.setUpdatedAt(batch.getUpdatedAt());
        return resp;
    }

    /** Convenience overload when related names are not needed. */
    public BatchResponse toResponse(Batch batch, long enrolledCount) {
        return toResponse(batch, Map.of(), Map.of(), enrolledCount);
    }
}
