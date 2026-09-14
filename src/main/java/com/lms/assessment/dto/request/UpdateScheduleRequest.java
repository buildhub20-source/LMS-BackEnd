package com.lms.assessment.dto.request;

import java.time.Instant;

public record UpdateScheduleRequest(
        Instant startTime,
        Instant endTime
) {}
