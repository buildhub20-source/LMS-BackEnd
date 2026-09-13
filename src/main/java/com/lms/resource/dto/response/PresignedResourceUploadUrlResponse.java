package com.lms.resource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedResourceUploadUrlResponse {
    private String uploadUrl;
    private String fileKey;
    private String publicUrl;
}
