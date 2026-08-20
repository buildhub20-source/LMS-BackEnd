package com.lms.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataSourceSecurityCheckTest {

    private static final String SUPABASE =
            "jdbc:postgresql://aws-0-eu-west-2.pooler.supabase.com:5432/postgres";

    @Test
    void aRemoteDatabaseWithoutTlsIsRefused() {
        assertThatThrownBy(() -> check(SUPABASE).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sslmode=require");
    }

    @Test
    void aRemoteDatabaseWithTlsIsAccepted() {
        assertThatCode(() -> check(SUPABASE + "?sslmode=require").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void strongerVerificationModesAreAccepted() {
        assertThatCode(() -> check(SUPABASE + "?sslmode=verify-full").afterPropertiesSet())
                .doesNotThrowAnyException();
        assertThatCode(() -> check(SUPABASE + "?sslmode=verify-ca").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void theCheckIsNotFooledByAWeakerMode() {
        assertThatThrownBy(() -> check(SUPABASE + "?sslmode=prefer").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void localhostIsExempt() {
        assertThatCode(() -> check("jdbc:postgresql://localhost:5432/lms").afterPropertiesSet())
                .doesNotThrowAnyException();
        assertThatCode(() -> check("jdbc:postgresql://127.0.0.1:5432/lms").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void theInMemoryTestDatabaseIsExempt() {
        assertThatCode(() -> check("jdbc:h2:mem:lms;MODE=PostgreSQL").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    private DataSourceSecurityCheck check(String url) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        return new DataSourceSecurityCheck(url, environment);
    }
}
