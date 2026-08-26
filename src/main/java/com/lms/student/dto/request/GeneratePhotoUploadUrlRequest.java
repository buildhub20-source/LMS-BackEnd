package com.lms.student.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Asks for a pre-signed URL to upload a student or guardian photo. */
@Data
public class GeneratePhotoUploadUrlRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "MIME type is required")
    private String mimeType;
}
