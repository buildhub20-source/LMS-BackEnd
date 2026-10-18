package com.lms.coderunner;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Outbound calls to a Judge0-compatible sandbox that compiles and runs student
 * code. Without a base URL the feature is off and "Run" explains that.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "lms.code-runner")
public class CodeRunnerConfig {

    /** e.g. http://judge0:2358. Blank turns the feature off. */
    private String baseUrl = "";

    /** Sent as X-Auth-Token when the sandbox requires one. */
    private String apiKey = "";

    private int timeoutMs = 20_000;

    /** Per-test-case limits enforced by the sandbox. */
    private double cpuTimeLimitSeconds = 3;
    private int memoryLimitKb = 256_000;

    /** Runs a single user may start per minute (each test case counts). */
    private int runsPerMinute = 30;

    private int maxInputsPerRequest = 10;

    /** Our language keys to Judge0 language ids; override to match the sandbox's image. */
    private Map<String, Integer> languageIds = new HashMap<>(Map.of(
            "java", 62,
            "python", 71,
            "cpp", 54,
            "c", 50,
            "javascript", 63));

    public boolean isEnabled() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    @Bean(name = "codeRunnerRestTemplate")
    public RestTemplate codeRunnerRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
