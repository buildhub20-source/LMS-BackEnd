package com.lms.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Configures the AWS SDK S3 client and pre-signer to talk to Cloudflare R2.
 *
 * <p>Cloudflare R2 is S3-compatible but requires:
 * <ul>
 *   <li>A custom endpoint override pointing at the account-specific URL.</li>
 *   <li>Region set to {@code auto} (R2 does not use AWS regions).</li>
 *   <li>Path-style access enabled (R2 does not support virtual-hosted style).</li>
 * </ul>
 *
 * <p>When credentials are absent (e.g. in tests), the beans are {@code null}
 * so the application context still loads; any feature that needs them will
 * fail at call-time with a clear message.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    private final CloudflareR2Properties r2;

    @Bean
    public S3Client s3Client() {
        if (!r2.isConfigured()) {
            log.warn("R2 credentials not provided — S3Client bean is null. "
                    + "File upload/download features will be unavailable.");
            return null;
        }

        return S3Client.builder()
                .endpointOverride(URI.create(r2.getEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.getAccessKey(), r2.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        if (!r2.isConfigured()) {
            return null;
        }

        return S3Presigner.builder()
                .endpointOverride(URI.create(r2.getEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.getAccessKey(), r2.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
