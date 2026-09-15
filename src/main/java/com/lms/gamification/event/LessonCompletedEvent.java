package com.lms.gamification.event;

import java.util.UUID;

/** Published when a student completes a lesson. */
public record LessonCompletedEvent(UUID studentId, UUID lessonId, UUID courseId) {}
