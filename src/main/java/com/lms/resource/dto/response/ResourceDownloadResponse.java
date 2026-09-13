package com.lms.resource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDownloadResponse {
    private String downloadUrl;
    private String fileName;
    private String fileType;
    private Integer downloadsCount;
}
