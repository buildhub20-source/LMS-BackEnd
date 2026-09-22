package com.lms.live.dto;

import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionStatus;

import java.time.Instant;
import java.util.UUID;

public record LiveSessionResponse(
        UUID id,
        UUID tenantId,
        UUID courseId,
        String courseTitle,
        UUID instructorId,
        String instructorName,
        String title,
        String description,
        Instant scheduledStart,
        Instant scheduledEnd,
        Instant actualStart,
        Instant actualEnd,
        String roomName,
        LiveSessionStatus status,
        String recordingUrl,
        Instant createdAt
) {
    public static LiveSessionResponse from(LiveSession session) {
        return new LiveSessionResponse(
                session.getId(),
                session.getTenantId(),
                session.getCourse() != null ? session.getCourse().getId() : null,
                session.getCourse() != null ? session.getCourse().getTitle() : null,
                session.getInstructor() != null ? session.getInstructor().getId() : null,
                session.getInstructor() != null ? session.getInstructor().getName() : null,
                session.getTitle(),
                session.getDescription(),
                session.getScheduledStart(),
                session.getScheduledEnd(),
                session.getActualStart(),
                session.getActualEnd(),
                session.getRoomName(),
                session.getStatus(),
                session.getRecordingUrl(),
                session.getCreatedAt()
        );
    }
}
