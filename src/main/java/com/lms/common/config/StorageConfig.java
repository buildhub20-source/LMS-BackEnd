package com.lms.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Slf4j
@Configuration
public class StorageConfig {

    @Value("${lms.storage.r2.endpoint:}")
    private String endpoint;

    @Value("${lms.storage.r2.access-key:}")
    private String accessKey;

    @Value("${lms.storage.r2.secret-key:}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        if (endpoint == null || endpoint.isBlank() || accessKey == null || accessKey.isBlank()) {
            log.warn("S3/R2 credentials not provided. Video upload features will fail.");
            return null; // Return null so context loads, but feature will fail at runtime if used
        }
        
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto")) // Cloudflare R2 uses 'auto' or 'us-east-1'
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        if (endpoint == null || endpoint.isBlank() || accessKey == null || accessKey.isBlank()) {
            return null;
        }
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }
}
