package com.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for outbound calls to the lms-certificate-service.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "lms.certificate-service")
public class CertificateServiceConfig {

    /**
     * Base URL of the certificate microservice.
     * Example: http://cert-service:8081
     */
    private String baseUrl = "http://localhost:8081";

    /**
     * When false, no webhook calls are made to the cert service.
     * Flip to true once the cert service is deployed.
     */
    private boolean enabled = false;

    /**
     * Timeout in milliseconds for outbound webhook calls.
     * Failures are non-fatal (fire-and-forget), so a short timeout is preferred.
     */
    private int timeoutMs = 3000;
}
