package com.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** Outbound calls to LMS-AI-Service. */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "lms.ai-service")
public class AiServiceConfig {

    private String baseUrl = "http://localhost:3003";
    private boolean enabled = false;
    private int timeoutMs = 180_000;

    /** Separate from the internal client: grading takes minutes, webhooks take seconds. */
    @Bean(name = "aiServiceRestTemplate")
    public RestTemplate aiServiceRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
