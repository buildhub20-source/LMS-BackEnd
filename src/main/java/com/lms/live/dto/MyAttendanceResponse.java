package com.lms.live.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A student's own live-class attendance across their enrolled courses. */
public record MyAttendanceResponse(
        int classesHeld,
        int classesAttended,
        double percentage,
        long minutesAttended,
        List<ClassRow> classes
) {
    public record ClassRow(
            UUID sessionId,
            String title,
            UUID courseId,
            String courseTitle,
            Instant scheduledStart,
            String status,
            boolean attended,
            long minutesAttended
    ) {}
}
