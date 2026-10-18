package com.lms.coderunner;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CodeRunnerTest {

    private static final String SUBMIT = "http://judge0.test/submissions?base64_encoded=true&wait=true";

    private CodeRunnerConfig config;
    private MockRestServiceServer sandbox;
    private CodeRunService service;
    private final UUID student = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        config = new CodeRunnerConfig();
        config.setBaseUrl("http://judge0.test");
        config.setApiKey("secret");
        RestTemplate restTemplate = new RestTemplate();
        sandbox = MockRestServiceServer.bindTo(restTemplate).build();
        service = new CodeRunService(config, new CodeRunnerClient(config, restTemplate));
    }

    @Test
    void runsEachInputAndDecodesTheSandboxOutput() {
        expectRun(71, "{\"status\":{\"id\":3},\"stdout\":\"" + b64("3\n") + "\",\"time\":\"0.012\",\"memory\":3400}");
        expectRun(71, "{\"status\":{\"id\":3},\"stdout\":\"" + b64("7\n") + "\",\"time\":\"0.010\",\"memory\":3300}");

        List<CodeRunResult> results = service.run(student, "python", "print(sum(map(int,input().split())))",
                List.of("1 2", "3 4"));

        sandbox.verify();
        assertThat(results).extracting(CodeRunResult::status).containsExactly("ACCEPTED", "ACCEPTED");
        assertThat(results).extracting(CodeRunResult::stdout).containsExactly("3\n", "7\n");
        assertThat(results.get(0).timeMs()).isEqualTo(12);
    }

    @Test
    void aCompileErrorIsReportedOnceNotPerInput() {
        expectRun(54, "{\"status\":{\"id\":6},\"compile_output\":\"" + b64("error: expected ';'") + "\"}");

        List<CodeRunResult> results = service.run(student, "cpp", "int main(){return 0}", List.of("a", "b", "c"));

        sandbox.verify();
        assertThat(results).singleElement()
                .satisfies(r -> {
                    assertThat(r.status()).isEqualTo("COMPILE_ERROR");
                    assertThat(r.compileOutput()).contains("expected ';'");
                });
    }

    @Test
    void timeoutsAndRuntimeErrorsAreLabelled() {
        expectRun(62, "{\"status\":{\"id\":5}}");
        expectRun(62, "{\"status\":{\"id\":11},\"stderr\":\"" + b64("Exception in thread main") + "\"}");

        List<CodeRunResult> results = service.run(student, "java", "public class Solution {}", List.of("x", "y"));

        assertThat(results).extracting(CodeRunResult::status)
                .containsExactly("TIME_LIMIT_EXCEEDED", "RUNTIME_ERROR");
        assertThat(results.get(1).stderr()).contains("Exception");
    }

    @Test
    void javaSolutionClassIsWrappedInMain() {
        String wrapped = CodeRunnerClient.asMainClass(
                "import java.util.*;\npublic class Solution {\n  public static void main(String[] a) {}\n}\n");

        assertThat(wrapped).contains("\nclass Solution {").contains("public class Main {").contains("Solution.main(args);");
        assertThat(CodeRunnerClient.asMainClass("public class Main { }")).isEqualTo("public class Main { }");
    }

    @Test
    void anUnreachableSandboxIsAServiceUnavailableError() {
        sandbox.expect(requestTo(SUBMIT)).andRespond(withServerError());

        assertThatThrownBy(() -> service.run(student, "c", "int main(){}", List.of("")))
                .isInstanceOfSatisfying(ApplicationException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE));
    }

    @Test
    void withoutASandboxRunsAreOffAndSaySo() {
        config.setBaseUrl("");

        assertThat(service.isAvailable()).isFalse();
        assertThatThrownBy(() -> service.run(student, "python", "print(1)", List.of()))
                .isInstanceOfSatisfying(ApplicationException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE));
    }

    @Test
    void eachUserHasAPerMinuteBudget() {
        config.setRunsPerMinute(2);
        expectRun(71, "{\"status\":{\"id\":3},\"stdout\":\"\"}");
        expectRun(71, "{\"status\":{\"id\":3},\"stdout\":\"\"}");
        service.run(student, "python", "pass", List.of("1", "2"));

        assertThatThrownBy(() -> service.run(student, "python", "pass", List.of("3")))
                .isInstanceOfSatisfying(ApplicationException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS));
    }

    @Test
    void tooManyInputsAreRejectedBeforeAnythingRuns() {
        config.setMaxInputsPerRequest(2);

        assertThatThrownBy(() -> service.run(student, "python", "pass", List.of("1", "2", "3")))
                .isInstanceOfSatisfying(ApplicationException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    private void expectRun(int languageId, String responseJson) {
        sandbox.expect(requestTo(SUBMIT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Auth-Token", "secret"))
                .andExpect(jsonPath("$.language_id").value(languageId))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private static String b64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
