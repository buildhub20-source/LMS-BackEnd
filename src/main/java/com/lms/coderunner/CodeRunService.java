package com.lms.coderunner;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs student code against a list of inputs in the external sandbox, with a
 * per-user budget so one tab cannot monopolise it.
 */
@Service
@RequiredArgsConstructor
public class CodeRunService {

    private final CodeRunnerConfig config;
    private final CodeRunnerClient client;
    private final Clock clock = Clock.systemUTC();
    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();

    public boolean isAvailable() {
        return config.isEnabled();
    }

    public List<CodeRunResult> run(UUID userId, String language, String sourceCode, List<String> inputs) {
        if (!config.isEnabled()) {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Running code in this language isn't set up on this server yet.");
        }
        List<String> cases = inputs == null || inputs.isEmpty() ? List.of("") : inputs;
        if (cases.size() > config.getMaxInputsPerRequest()) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED,
                    "Run at most " + config.getMaxInputsPerRequest() + " inputs at a time.");
        }
        reserve(userId, cases.size());

        List<CodeRunResult> results = new ArrayList<>(cases.size());
        for (String input : cases) {
            CodeRunResult result = client.run(language, sourceCode, input);
            results.add(result);
            // A compile error is the same for every input; don't spend the budget repeating it.
            if ("COMPILE_ERROR".equals(result.status())) break;
        }
        return results;
    }

    private void reserve(UUID userId, int runs) {
        long minute = Instant.now(clock).getEpochSecond() / 60;
        Window window = windows.compute(userId, (id, current) ->
                current == null || current.minute != minute ? new Window(minute, runs) : current.add(runs));
        if (window.count > config.getRunsPerMinute()) {
            throw new ApplicationException(ErrorCode.TOO_MANY_REQUESTS,
                    "You've run code a lot in the last minute. Wait a moment and try again.");
        }
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> e.getValue().minute != minute);
        }
    }

    private record Window(long minute, int count) {
        Window add(int runs) {
            return new Window(minute, count + runs);
        }
    }
}
