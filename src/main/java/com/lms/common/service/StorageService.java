package com.lms.common.service;

import com.lms.common.config.CloudflareR2Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Service
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final CloudflareR2Properties r2;

    public StorageService(
            @org.springframework.beans.factory.annotation.Autowired(required = false) S3Client s3Client,
            @org.springframework.beans.factory.annotation.Autowired(required = false) S3Presigner s3Presigner,
            CloudflareR2Properties r2) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.r2 = r2;
    }

    /**
     * Directly uploads a file input stream to Cloudflare R2 using the S3 client.
     */
    public void uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        if (s3Client == null) {
            log.warn("S3Client is not configured. Skipping actual R2 upload.");
            return;
        }

        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(r2.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(objectRequest, RequestBody.fromInputStream(inputStream, contentLength));
            log.info("Successfully uploaded object to R2 bucket {}: {}", r2.getBucket(), key);
        } catch (Exception e) {
            log.error("Cloudflare R2 upload error for bucket {} key {}: {}", r2.getBucket(), key, e.getMessage(), e);
            throw new com.lms.common.exception.ApplicationException(
                    com.lms.common.exception.ErrorCode.INTERNAL_ERROR,
                    "Cloudflare R2 Storage Upload Failed: " + e.getMessage());
        }
    }


    /**
     * Retrieves an object input stream directly from Cloudflare R2 for streaming.
     */
    public software.amazon.awssdk.core.ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> getObjectStream(String key) {
        if (s3Client == null) {
            log.warn("S3Client is not configured. Cannot stream object from R2.");
            return null;
        }
        try {
            software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest = 
                    software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                    .bucket(r2.getBucket())
                    .key(key)
                    .build();
            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            log.error("Failed to retrieve stream from R2 for key {}: {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generates a pre-signed GET URL for downloading or playing an object from R2.
     */
    public String generatePresignedGetUrl(String key) {
        if (s3Presigner == null) {
            return null;
        }
        try {
            software.amazon.awssdk.services.s3.model.GetObjectRequest objectRequest = 
                    software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                    .bucket(r2.getBucket())
                    .key(key)
                    .build();
            software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest presignRequest = 
                    software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(2))
                    .getObjectRequest(objectRequest)
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned GET URL for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Generates a pre-signed PUT URL for uploading a file directly to R2.
     * @param key The object key (e.g. "courses/{courseId}/lessons/{lessonId}/{uuid}.mp4")
     * @param contentType The MIME type of the file
     * @return The pre-signed URL string, or null if unconfigured
     */
    public String generatePresignedUploadUrl(String key, String contentType) {
        if (s3Presigner == null) {
            log.warn("S3 presigner is not configured. Skipping presigned URL generation.");
            return null;
        }

        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(r2.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))
                    .putObjectRequest(objectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Returns the public URL for the given key, if public read is configured.
     */
    public String getPublicUrl(String key) {
        String url = r2.getPublicUrl();
        if (url == null || url.isBlank()) {
            return null;
        }
        return url.endsWith("/") ? url + key : url + "/" + key;
    }
}
