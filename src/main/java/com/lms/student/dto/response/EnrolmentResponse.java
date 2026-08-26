package com.lms.student.dto.response;

import com.lms.student.entity.BatchStatus;
import com.lms.student.entity.EnrolmentStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** A learner's membership of one batch, flattened for the client. */
@Data
public class EnrolmentResponse {

    private UUID id;
    private UUID batchId;
    private String batchCode;
    private String batchName;
    private BatchStatus batchStatus;
    private LocalDate enrolledOn;
    private EnrolmentStatus status;
    private LocalDate completedOn;
}
