package com.lms.resource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampusResourceResponse {
    private UUID id;
    private String title;
    private String description;
    private String category;
    private String fileKey;
    private String fileName;
    private String fileType;
    private String fileSize;
    private Long fileSizeBytes;
    private String targetAudience;
    private UUID authorId;
    private String authorName;
    private String authorRole;
    private Integer downloadsCount;
    private String status;
    private String storageProvider;
    private String downloadUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
