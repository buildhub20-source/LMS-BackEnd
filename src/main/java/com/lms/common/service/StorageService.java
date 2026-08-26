package com.lms.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Slf4j
@Service
public class StorageService {

    private final S3Presigner s3Presigner;

    public StorageService(@org.springframework.beans.factory.annotation.Autowired(required = false) S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

    @Value("${lms.storage.r2.bucket:lms-bucket}")
    private String bucket;

    @Value("${lms.storage.r2.public-url:}")
    private String publicUrl;

    /**
     * Generates a pre-signed PUT URL for uploading a file directly to R2.
     * @param key The object key (e.g. "courses/{courseId}/lessons/{lessonId}/{uuid}.mp4")
     * @param contentType The MIME type of the file
     * @return The pre-signed URL string
     */
    public String generatePresignedUploadUrl(String key, String contentType) {
        if (s3Presigner == null) {
            log.warn("S3 presigner is not configured. Returning fallback URL.");
            return "http://localhost:8080/fallback-upload/" + key; // Just a fallback for missing config
        }

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Returns the public URL for the given key, if public read is configured.
     */
    public String getPublicUrl(String key) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        return publicUrl.endsWith("/") ? publicUrl + key : publicUrl + "/" + key;
    }
}
