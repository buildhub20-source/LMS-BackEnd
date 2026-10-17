package com.lms.assessment.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.config.AiServiceConfig;
import com.lms.config.InternalApiConfig;
import com.lms.security.authorization.ServiceKeyAuthFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/** Calls LMS-AI-Service. Every failure surfaces as {@link AiGradingException}. */
@Slf4j
@Component
public class AiGradingClient {

    private final AiServiceConfig config;
    private final InternalApiConfig internalApiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiGradingClient(AiServiceConfig config, InternalApiConfig internalApiConfig,
                           @Qualifier("aiServiceRestTemplate") RestTemplate restTemplate,
                           ObjectMapper objectMapper) {
        this.config = config;
        this.internalApiConfig = internalApiConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    /** Returns the service's {@code data} object: score, test results, rubric, feedback, usage. */
    public JsonNode evaluate(GradeRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(ServiceKeyAuthFilter.SERVICE_KEY_HEADER, internalApiConfig.getServiceKey());
        String url = config.getBaseUrl() + "/api/v1/internal/grading/evaluate";
        try {
            JsonNode body = restTemplate.postForObject(url, new HttpEntity<>(request, headers), JsonNode.class);
            if (body == null || !body.hasNonNull("data")) {
                throw new AiGradingException("AI_BAD_RESPONSE", "The AI service returned no result", false);
            }
            return body.get("data");
        } catch (HttpStatusCodeException ex) {
            throw fromErrorResponse(ex);
        } catch (ResourceAccessException ex) {
            log.warn("AI service unreachable at {}: {}", config.getBaseUrl(), ex.getMessage());
            throw new AiGradingException("AI_UNAVAILABLE", "The AI service could not be reached", true);
        }
    }

    private AiGradingException fromErrorResponse(HttpStatusCodeException ex) {
        String code = "AI_ERROR";
        String message = "The AI service could not grade this answer";
        try {
            JsonNode error = objectMapper.readTree(ex.getResponseBodyAsString()).path("error");
            code = error.path("code").asText(code);
            message = error.path("message").asText(message);
        } catch (Exception ignored) {
            // Keep the generic code and message.
        }
        boolean retryable = ex.getStatusCode().is5xxServerError();
        log.warn("AI service returned {} {}: {}", ex.getStatusCode().value(), code, message);
        return new AiGradingException(code, message, retryable);
    }

    public record GradeRequest(Question question, List<TestCase> testCases, List<Criterion> rubric,
                               Submission submission) {}

    public record Question(String title, String description, String inputFormat, String outputFormat,
                           String constraints, int marks) {}

    public record TestCase(String id, String inputData, String expectedOutput, boolean hidden, int weight) {}

    public record Criterion(String id, String criterionName, int maxPoints) {}

    public record Submission(String language, String sourceCode) {}
}
