package com.lms.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pre-signed upload target. The client PUTs the file to {@code uploadUrl} and
 * then submits {@code photoKey} with the student or guardian payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPhotoUploadUrlResponse {

    private String uploadUrl;
    private String photoKey;
}
