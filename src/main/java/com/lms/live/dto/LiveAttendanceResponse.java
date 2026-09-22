package com.lms.live.dto;

import com.lms.live.entity.LiveSessionAttendance;

import java.time.Instant;
import java.util.UUID;

public record LiveAttendanceResponse(
        UUID id,
        UUID sessionId,
        UUID userId,
        String userName,
        String userEmail,
        Instant joinedAt,
        Instant leftAt,
        long durationSeconds
) {
    public static LiveAttendanceResponse from(LiveSessionAttendance attendance) {
        return new LiveAttendanceResponse(
                attendance.getId(),
                attendance.getSession() != null ? attendance.getSession().getId() : null,
                attendance.getUser() != null ? attendance.getUser().getId() : null,
                attendance.getUser() != null ? attendance.getUser().getName() : null,
                attendance.getUser() != null ? attendance.getUser().getEmail() : null,
                attendance.getJoinedAt(),
                attendance.getLeftAt(),
                attendance.getDurationSeconds()
        );
    }
}
