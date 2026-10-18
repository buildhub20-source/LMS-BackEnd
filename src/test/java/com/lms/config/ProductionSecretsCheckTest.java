package com.lms.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSecretsCheckTest {

    private static final String STRONG = "k3Jx9vQ2mWp7Lr5Tz8Ny4Bh6Fd1Gs0Ca";

    @Test
    void realValuesStartInProduction() {
        assertThatCode(() -> ProductionSecretsCheck.verify(prod())).doesNotThrowAnyException();
    }

    @Test
    void theOldDevelopmentDefaultsAreRefused() {
        MockEnvironment env = prod()
                .withProperty("lms.internal.service-key", "change-me-in-production")
                .withProperty("lms.livekit.api-key", "devkey")
                .withProperty("lms.livekit.api-secret", "secret12345678901234567890123456789012")
                .withProperty("lms.livekit.url", "wss://livekit.cloud.local");

        assertThatThrownBy(() -> ProductionSecretsCheck.verify(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SERVICE_KEY_SECRET")
                .hasMessageContaining("LIVEKIT_API_KEY")
                .hasMessageContaining("LIVEKIT_API_SECRET")
                .hasMessageContaining("LIVEKIT_URL");
    }

    @Test
    void aShortSecretIsRefused() {
        MockEnvironment env = prod().withProperty("lms.security.jwt.secret", "short");

        assertThatThrownBy(() -> ProductionSecretsCheck.verify(env))
                .hasMessageContaining("JWT_SECRET must be at least 32 characters");
    }

    @Test
    void aMissingSecretIsReportedByName() {
        MockEnvironment env = prod().withProperty("lms.livekit.api-secret", "${LIVEKIT_API_SECRET}");

        assertThatThrownBy(() -> ProductionSecretsCheck.verify(env))
                .hasMessageContaining("LIVEKIT_API_SECRET is not set");
    }

    @Test
    void otherProfilesAreNotChecked() {
        MockEnvironment env = new MockEnvironment().withProperty("lms.livekit.api-key", "devkey");
        env.setActiveProfiles("dev");

        assertThatCode(() -> ProductionSecretsCheck.verify(env)).doesNotThrowAnyException();
    }

    private MockEnvironment prod() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("lms.security.jwt.secret", STRONG)
                .withProperty("lms.internal.service-key", STRONG)
                .withProperty("lms.livekit.api-key", "APIa8Bx2Lk")
                .withProperty("lms.livekit.api-secret", STRONG)
                .withProperty("lms.livekit.url", "wss://gradusmind.livekit.cloud");
        env.setActiveProfiles("prod");
        return env;
    }
}
