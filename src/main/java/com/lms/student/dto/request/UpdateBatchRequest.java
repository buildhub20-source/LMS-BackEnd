package com.lms.student.dto.request;

import com.lms.student.entity.BatchStatus;
import com.lms.student.entity.DeliveryMode;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** Partial update of a batch; a null field is left alone. */
@Data
public class UpdateBatchRequest {

    @Size(max = 150, message = "Batch name must not exceed 150 characters")
    private String name;

    private UUID courseId;

    private UUID instructorId;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 150, message = "Schedule must not exceed 150 characters")
    private String schedule;

    private DeliveryMode deliveryMode;

    @Positive(message = "Capacity must be greater than zero")
    private Integer capacity;

    private BatchStatus status;
}
