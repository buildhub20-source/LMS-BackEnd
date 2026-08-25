package com.lms.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class GenerateUploadUrlRequest {
    
    @NotBlank(message = "File name is required")
    private String fileName;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;

    @NotBlank(message = "MIME type is required")
    private String mimeType;
}
