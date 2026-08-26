package com.lms.student.dto.request;

import com.lms.student.entity.BatchStatus;
import com.lms.student.entity.DeliveryMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** Payload for scheduling a batch. */
@Data
public class CreateBatchRequest {

    @NotBlank(message = "Batch code is required")
    @Size(max = 50, message = "Batch code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Batch name is required")
    @Size(max = 150, message = "Batch name must not exceed 150 characters")
    private String name;

    /** Optional: a batch can be scheduled before its course exists. */
    private UUID courseId;

    private UUID instructorId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 150, message = "Schedule must not exceed 150 characters")
    private String schedule;

    private DeliveryMode deliveryMode;

    @Positive(message = "Capacity must be greater than zero")
    private Integer capacity;

    private BatchStatus status;
}
