package com.lms.student.dto.request;

import com.lms.student.entity.EnrolmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** One batch a learner is being enrolled into. */
@Data
public class EnrolmentRequest {

    @NotNull(message = "Batch is required")
    private UUID batchId;

    /** Defaults to today when omitted. */
    private LocalDate enrolledOn;

    /** Defaults to ACTIVE when omitted. */
    private EnrolmentStatus status;
}
