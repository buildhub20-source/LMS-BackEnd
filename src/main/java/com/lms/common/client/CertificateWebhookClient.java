package com.lms.common.client;

import com.lms.config.CertificateServiceConfig;
import com.lms.config.InternalApiConfig;
import com.lms.security.authorization.ServiceKeyAuthFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Fire-and-forget HTTP client that notifies the lms-certificate-service when
 * an enrollment transitions to {@code COMPLETED}.
 *
 * <p>The call is {@link Async} so that it never blocks or delays the enrollment
 * transaction. Any failure (network, timeout, cert service down) is logged as a
 * warning but does not propagate — the enrollment itself is already committed.
 * The admin can always issue the certificate manually if the webhook is missed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateWebhookClient {

    private final CertificateServiceConfig certConfig;
    private final InternalApiConfig internalApiConfig;
    private final RestTemplate internalRestTemplate;

    /**
     * Notifies the cert service to auto-issue a certificate for the given
     * student + course pair.
     *
    * @param studentId the student who completed the course
    * @param courseId  the course that was completed
     * @param tenantSlug active tenant whose dedicated database contains the enrollment
     */
    @Async
    public void notifyEnrollmentCompleted(UUID studentId, UUID courseId, String tenantSlug) {
        if (!certConfig.isEnabled()) {
            log.debug("Certificate service integration is disabled — skipping webhook for student={} course={}",
                    studentId, courseId);
            return;
        }

        try {
            String url = certConfig.getBaseUrl() + "/api/v1/internal/events/enrollment-completed";

            HttpHeaders headers = new HttpHeaders();
            headers.set(ServiceKeyAuthFilter.SERVICE_KEY_HEADER, internalApiConfig.getServiceKey());
            headers.set("Content-Type", "application/json");
            if (tenantSlug != null && !tenantSlug.isBlank()) {
                headers.set("X-Tenant-Slug", tenantSlug);
            }

            Map<String, String> body = Map.of(
                    "studentId", studentId.toString(),
                    "courseId", courseId.toString(),
                    "tenantSlug", tenantSlug == null ? "" : tenantSlug
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            internalRestTemplate.exchange(url, HttpMethod.POST, request, Void.class);

            log.info("Enrollment-completed webhook sent to cert service: student={} course={}",
                    studentId, courseId);

        } catch (Exception ex) {
            // Non-fatal — the enrollment is already committed.
            log.warn("Failed to send enrollment-completed webhook to cert service: student={} course={} — {}",
                    studentId, courseId, ex.getMessage());
        }
    }
}
