package com.lms.resource.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCampusResourceRequest {
    private String title;
    private String description;
    private String category;
    private String targetAudience;
    private String status;
    private String fileKey;
    private String fileName;
    private String fileType;
    private String fileSize;
    private Long fileSizeBytes;
}
