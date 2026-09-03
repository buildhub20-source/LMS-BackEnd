package com.lms.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the internal service-to-service API and the
 * certificate microservice webhook integration.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "lms.internal")
public class InternalApiConfig {

    /**
     * Shared secret transmitted in the {@code X-Service-Key} header by any
     * trusted internal caller (e.g. lms-certificate-service).
     * Must be at least 32 characters and treated like a password.
     */
    @NotBlank
    private String serviceKey;
}
