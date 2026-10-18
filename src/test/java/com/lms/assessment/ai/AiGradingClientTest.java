package com.lms.assessment.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.config.AiServiceConfig;
import com.lms.config.InternalApiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiGradingClientTest {

    private static final String URL = "http://ai.test/api/v1/internal/grading/evaluate";
    private static final AiGradingClient.GradeRequest REQUEST = new AiGradingClient.GradeRequest(
            new AiGradingClient.Question("Sum", "Add two numbers", "", "", "", 10),
            List.of(new AiGradingClient.TestCase("t1", "1 2", "3", false, 1)),
            List.of(),
            new AiGradingClient.Submission("PYTHON", "print(3)"));

    private MockRestServiceServer server;
    private AiGradingClient client;

    @BeforeEach
    void setUp() {
        AiServiceConfig config = new AiServiceConfig();
        config.setBaseUrl("http://ai.test");
        config.setEnabled(true);
        InternalApiConfig internal = new InternalApiConfig();
        internal.setServiceKey("service-key-for-tests-0123456789abcdef");
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new AiGradingClient(config, internal, restTemplate, new ObjectMapper());
    }

    @Test
    void sendsTheServiceKeyAndReturnsTheResult() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Service-Key", "service-key-for-tests-0123456789abcdef"))
                .andExpect(jsonPath("$.question.marks").value(10))
                .andExpect(jsonPath("$.testCases[0].expectedOutput").value("3"))
                .andRespond(withSuccess("{\"data\":{\"score\":7.5,\"maxScore\":10}}", MediaType.APPLICATION_JSON));

        assertThat(client.evaluate(REQUEST).path("score").asDouble()).isEqualTo(7.5);
        server.verify();
    }

    @Test
    void aRefusalIsNotRetryable() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":\"GRADING_REFUSED\",\"message\":\"The submission could not be graded automatically\"}}"));

        assertThatThrownBy(() -> client.evaluate(REQUEST))
                .isInstanceOfSatisfying(AiGradingException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("GRADING_REFUSED");
                    assertThat(ex.isRetryable()).isFalse();
                    assertThat(ex.getMessage()).contains("could not be graded");
                });
    }

    @Test
    void aBusyServiceIsRetryable() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":\"AI_RATE_LIMITED\",\"message\":\"AI grading is busy, try again shortly\"}}"));

        assertThatThrownBy(() -> client.evaluate(REQUEST))
                .isInstanceOfSatisfying(AiGradingException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("AI_RATE_LIMITED");
                    assertThat(ex.isRetryable()).isTrue();
                });
    }

    @Test
    void aResponseWithoutDataIsAnError() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.evaluate(REQUEST))
                .isInstanceOfSatisfying(AiGradingException.class, ex -> assertThat(ex.getCode()).isEqualTo("AI_BAD_RESPONSE"));
    }
}
