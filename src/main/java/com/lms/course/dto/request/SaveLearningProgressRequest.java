package com.lms.course.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record SaveLearningProgressRequest(@NotNull Set<@NotNull UUID> completedLessonIds) {}
