package com.lms.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for Cloudflare R2 object storage.
 *
 * <p>Bound to the {@code lms.storage.r2} prefix in application.yml.
 * All values are resolved from environment variables via the .env file
 * (which is git-ignored). Never log or expose {@link #secretKey}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "lms.storage.r2")
public class CloudflareR2Properties {

    /** Cloudflare account ID — used to derive the R2 endpoint if needed. */
    private String accountId;

    /** Full S3-compatible endpoint URL, e.g. https://{accountId}.r2.cloudflarestorage.com */
    private String endpoint;

    /** R2 API token access key (Object Read & Write). */
    private String accessKey;

    /** R2 API token secret key. NEVER log or print this value. */
    private String secretKey;

    /** R2 bucket name, e.g. buildhub-lms. */
    private String bucket;

    /** Optional CDN / public URL prefix for publicly readable objects. */
    private String publicUrl;

    /** Returns true when the minimum credentials needed to build an S3 client are present. */
    public boolean isConfigured() {
        return endpoint != null && !endpoint.isBlank()
                && accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
