package com.lms.course.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Seconds the lesson page was visible since the previous heartbeat. */
public record LearningHeartbeatRequest(@Min(0) @Max(600) int seconds) {}
