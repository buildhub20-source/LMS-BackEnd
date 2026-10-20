package com.lms.coderunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Talks to a Judge0-compatible sandbox: one synchronous submission per input. */
@Slf4j
@Component
public class CodeRunnerClient {

    private static final Pattern PUBLIC_CLASS = Pattern.compile("public\\s+(?:final\\s+)?class\\s+(\\w+)");

    private final CodeRunnerConfig config;
    private final RestTemplate restTemplate;

    public CodeRunnerClient(CodeRunnerConfig config, @Qualifier("codeRunnerRestTemplate") RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public CodeRunResult run(String language, String sourceCode, String stdin) {
        Integer languageId = config.getLanguageIds().get(language);
        if (languageId == null) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED, "Unsupported language: " + language);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("language_id", languageId);
        body.put("source_code", encode("java".equals(language) ? asMainClass(sourceCode) : sourceCode));
        body.put("stdin", encode(stdin == null ? "" : stdin));
        body.put("cpu_time_limit", config.getCpuTimeLimitSeconds());
        body.put("memory_limit", config.getMemoryLimitKb());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            headers.set("X-Auth-Token", config.getApiKey());
        }

        String url = config.getBaseUrl() + "/submissions?base64_encoded=true&wait=true";
        try {
            JsonNode response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), JsonNode.class);
            if (response == null) {
                throw unavailable(null);
            }
            return toResult(response);
        } catch (RestClientException ex) {
            log.warn("Code runner call failed: {}", ex.getMessage());
            throw unavailable(ex);
        }
    }

    /**
     * Judge0 compiles Java as Main.java, while our starter code (and most
     * students) use {@code public class Solution}. Make that class package-private
     * and add a Main that delegates to it.
     */
    static String asMainClass(String source) {
        if (source.matches("(?s).*\\bclass\\s+Main\\b.*")) {
            return source;
        }
        Matcher matcher = PUBLIC_CLASS.matcher(source);
        if (!matcher.find()) {
            return source;
        }
        String name = matcher.group(1);
        String withoutPublic = source.substring(0, matcher.start())
                + source.substring(matcher.start()).replaceFirst("public\\s+", "");
        return withoutPublic + "\n\npublic class Main {\n"
                + "    public static void main(String[] args) throws Exception {\n"
                + "        " + name + ".main(args);\n"
                + "    }\n}\n";
    }

    private static CodeRunResult toResult(JsonNode response) {
        int statusId = response.path("status").path("id").asInt(13);
        String status = switch (statusId) {
            case 3 -> "ACCEPTED";
            case 5 -> "TIME_LIMIT_EXCEEDED";
            case 6 -> "COMPILE_ERROR";
            case 7, 8, 9, 10, 11, 12 -> "RUNTIME_ERROR";
            default -> "SANDBOX_ERROR";
        };
        Integer timeMs = response.hasNonNull("time")
                ? (int) Math.round(response.path("time").asDouble() * 1000)
                : null;
        Integer memoryKb = response.hasNonNull("memory") ? response.path("memory").asInt() : null;
        String stderr = decode(response.path("stderr"));
        if (stderr == null && "RUNTIME_ERROR".equals(status)) {
            stderr = decode(response.path("message"));
        }
        return new CodeRunResult(status, decode(response.path("stdout")), stderr,
                decode(response.path("compile_output")), timeMs, memoryKb);
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        try {
            return new String(Base64.getMimeDecoder().decode(node.asText()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return node.asText();
        }
    }

    private static ApplicationException unavailable(Throwable cause) {
        return new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE,
                "The code runner is not reachable right now. Try again in a moment.", cause);
    }
}
