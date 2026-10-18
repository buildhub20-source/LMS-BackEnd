package com.lms.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Refuses to start in the {@code prod} profile while a secret is missing or
 * still holds a development placeholder. Runs as soon as configuration is
 * loaded, before any bean (or database connection) is created.
 */
public class ProductionSecretsCheck implements EnvironmentPostProcessor, Ordered {

    static final int MIN_SECRET_LENGTH = 32;

    private static final List<String> PLACEHOLDER_MARKERS = List.of(
            "change-me", "changeme", "change_me", "devkey", "secret1234", "placeholder", "your-");

    private static final Map<String, String> SECRETS = Map.of(
            "JWT_SECRET", "lms.security.jwt.secret",
            "SERVICE_KEY_SECRET", "lms.internal.service-key",
            "LIVEKIT_API_SECRET", "lms.livekit.api-secret");

    @Override
    public int getOrder() {
        // After the config-data processor has loaded application-prod.yml.
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        verify(environment);
    }

    static void verify(Environment environment) {
        if (!environment.matchesProfiles("prod")) {
            return;
        }

        List<String> problems = new ArrayList<>();
        SECRETS.forEach((name, property) -> {
            String value = read(environment, property);
            if (value.isBlank()) {
                problems.add(name + " is not set");
            } else if (looksLikePlaceholder(value)) {
                problems.add(name + " is a development placeholder");
            } else if (value.length() < MIN_SECRET_LENGTH) {
                problems.add(name + " must be at least " + MIN_SECRET_LENGTH + " characters");
            }
        });

        String apiKey = read(environment, "lms.livekit.api-key");
        if (apiKey.isBlank()) {
            problems.add("LIVEKIT_API_KEY is not set");
        } else if (looksLikePlaceholder(apiKey)) {
            problems.add("LIVEKIT_API_KEY is a development placeholder");
        }

        String liveKitUrl = read(environment, "lms.livekit.url").toLowerCase(Locale.ROOT);
        if (!liveKitUrl.startsWith("wss://") || liveKitUrl.contains(".local")) {
            problems.add("LIVEKIT_URL must be the wss:// address of a real LiveKit server");
        }

        if (!problems.isEmpty()) {
            problems.sort(String::compareTo);
            throw new IllegalStateException("Refusing to start in production: " + String.join("; ", problems));
        }
    }

    private static String read(Environment environment, String property) {
        try {
            return environment.getProperty(property, "");
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            return "";
        }
    }

    private static boolean looksLikePlaceholder(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return PLACEHOLDER_MARKERS.stream().anyMatch(lower::contains);
    }
}
