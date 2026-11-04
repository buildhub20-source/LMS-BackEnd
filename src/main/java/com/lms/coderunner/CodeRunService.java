package com.lms.coderunner;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.sharedstate.InMemoryRateLimitStore;
import com.lms.common.sharedstate.RateLimitStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs student code against a list of inputs in the external sandbox, with a
 * per-user budget so one tab cannot monopolise it.
 */
@Service
public class CodeRunService {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final CodeRunnerConfig config;
    private final CodeRunnerClient client;
    /** Shared across backend copies when Redis is enabled, so the budget does not multiply. */
    private final RateLimitStore store;

    @Autowired
    public CodeRunService(CodeRunnerConfig config, CodeRunnerClient client, RateLimitStore store) {
        this.config = config;
        this.client = client;
        this.store = store;
    }

    CodeRunService(CodeRunnerConfig config, CodeRunnerClient client) {
        this(config, client, new InMemoryRateLimitStore());
    }

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

        // If stopped early due to compile error, refund the unexecuted runs
        if (results.size() < cases.size()) {
            refund(userId, cases.size() - results.size());
        }

        return results;
    }

    private void reserve(UUID userId, int runs) {
        RateLimitStore.Hit hit = store.add(key(userId), runs, WINDOW);
        if (hit.count() > config.getRunsPerMinute()) {
            // A refused request does not use up the budget.
            store.add(key(userId), -runs, WINDOW);
            throw new ApplicationException(ErrorCode.TOO_MANY_REQUESTS,
                    "You've run code a lot in the last minute. Wait a moment and try again.");
        }
    }

    private void refund(UUID userId, int unexecuted) {
        store.add(key(userId), -unexecuted, WINDOW);
    }

    private static String key(UUID userId) {
        return "coderun:" + userId;
    }
}
