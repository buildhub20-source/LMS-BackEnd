package com.lms.common.service;

import com.lms.common.config.CloudflareR2Properties;
import com.lms.platform.runtime.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
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
     * Resolves the multi-tenant prefix for object keys.
     * Guarantees all tenant files are isolated under "tenants/{tenantId}/".
     */
    public String tenantPrefix() {
        return TenantContext.current()
                .map(tenant -> "tenants/" + tenant.tenantId() + "/")
                .orElse("tenants/shared/");
    }

    /**
     * Ensures an object key is scoped to the current tenant namespace.
     * If the key already starts with "tenants/", returns it as-is.
     *
     * @param path relative path (e.g. "courses/123/video.mp4")
     * @return scoped key (e.g. "tenants/{tenantId}/courses/123/video.mp4")
     */
    public String scopedKey(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        String clean = path.startsWith("/") ? path.substring(1) : path;
        if (clean.startsWith("tenants/")) {
            return clean;
        }
        return tenantPrefix() + clean;
    }

    /**
     * Directly uploads a file input stream to Cloudflare R2 using the S3 client.
     */
    public void uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        if (s3Client == null) {
            throw new com.lms.common.exception.ApplicationException(
                    com.lms.common.exception.ErrorCode.INTERNAL_ERROR,
                    "Cloudflare R2 storage is not configured");
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

    /** Returns whether an uploaded object is available in the configured R2 bucket. */
    public boolean objectExists(String key) {
        if (s3Client == null) {
            log.warn("S3Client is not configured. Cannot verify R2 object {}.", key);
            return false;
        }

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(r2.getBucket())
                    .key(key)
                    .build());
            return true;
        } catch (Exception e) {
            log.warn("R2 object is not available for bucket {} key {}: {}",
                    r2.getBucket(), key, e.getMessage());
            return false;
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
        return generatePresignedGetUrl(key, null);
    }

    /**
     * Generates a pre-signed GET URL for downloading with Content-Disposition attachment header.
     */
    public String generatePresignedGetUrl(String key, String fileName) {
        if (s3Presigner == null) {
            return null;
        }
        try {
            software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder objectRequestBuilder =
                    software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                    .bucket(r2.getBucket())
                    .key(key);

            if (fileName != null && !fileName.isBlank()) {
                String safeName = fileName.replace("\"", "").trim();
                objectRequestBuilder.responseContentDisposition("attachment; filename=\"" + safeName + "\"");
            }

            software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest presignRequest =
                    software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(2))
                    .getObjectRequest(objectRequestBuilder.build())
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

    /**
     * Deletes an object from Cloudflare R2 bucket.
     */
    public void deleteObject(String key) {
        if (s3Client == null || key == null || key.isBlank()) {
            return;
        }
        try {
            software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteRequest =
                    software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                            .bucket(r2.getBucket())
                            .key(key)
                            .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted object from R2 bucket {}: {}", r2.getBucket(), key);
        } catch (Exception e) {
            log.warn("Failed to delete object from R2 bucket {} for key {}: {}", r2.getBucket(), key, e.getMessage());
        }
    }
}
