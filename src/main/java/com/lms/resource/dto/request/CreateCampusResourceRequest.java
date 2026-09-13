package com.lms.resource.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampusResourceRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "File key is required")
    private String fileKey;

    @NotBlank(message = "File name is required")
    private String fileName;

    private String fileType;
    private String fileSize;
    private Long fileSizeBytes;
    private String targetAudience;
    private String status;
}
