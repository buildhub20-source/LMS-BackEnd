package com.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client configuration for outbound service-to-service calls.
 *
 * <p>{@code EnableAsync} is placed here so that {@code @Async} webhook calls
 * (e.g. {@link com.lms.common.client.CertificateWebhookClient}) are
 * dispatched on Spring's shared task executor rather than on the request thread.
 */
@Configuration
@EnableAsync
public class HttpClientConfig {

    /**
     * A {@link RestTemplate} configured with short timeouts for internal
     * service-to-service calls. Failures must not block the caller's transaction.
     */
    @Bean(name = "internalRestTemplate")
    public RestTemplate internalRestTemplate(CertificateServiceConfig certConfig) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(certConfig.getTimeoutMs());
        factory.setReadTimeout(certConfig.getTimeoutMs());
        return new RestTemplate(factory);
    }
}
