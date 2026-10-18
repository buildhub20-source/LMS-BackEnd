package com.lms.announcement.dto;

import com.lms.announcement.entity.Announcement;
import com.lms.announcement.entity.AnnouncementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AnnouncementResponse {

    private UUID id;
    private String title;
    private String message;
    private AnnouncementType type;
    private boolean active;
    private Instant expiresAt;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public static AnnouncementResponse from(Announcement a) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .message(a.getMessage())
                .type(a.getType())
                .active(a.isActive())
                .expiresAt(a.getExpiresAt())
                .createdBy(a.getCreatedBy())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
