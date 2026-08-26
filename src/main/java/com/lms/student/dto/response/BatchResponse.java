package com.lms.student.dto.response;

import com.lms.student.entity.BatchStatus;
import com.lms.student.entity.DeliveryMode;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class BatchResponse {

    private UUID id;
    private String code;
    private String name;

    private UUID courseId;
    private String courseTitle;

    private UUID instructorId;
    private String instructorName;

    private LocalDate startDate;
    private LocalDate endDate;
    private String schedule;
    private DeliveryMode deliveryMode;
    private Integer capacity;
    private BatchStatus status;

    /** Learners currently enrolled, so the UI can show "12 / 20". */
    private long enrolledCount;

    private Instant createdAt;
    private Instant updatedAt;
}
