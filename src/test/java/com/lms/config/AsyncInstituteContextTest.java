package com.lms.config;

import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves Spring Boot applies the decorator to the executor that runs
 * {@code @Async} methods (learning and reminder mails), not just that the
 * decorator works on its own.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("@Async work keeps the institute")
class AsyncInstituteContextTest {

    @Autowired
    private ThreadPoolTaskExecutor applicationTaskExecutor;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void asyncExecutorCarriesInstitute() throws Exception {
        TenantContext.set(new TenantConnection(UUID.randomUUID(), "college", "Test Institute",
                "jdbc:postgresql://db.invalid/postgres", "user", "secret"));

        String seen = applicationTaskExecutor.submit(
                () -> TenantContext.current().map(TenantConnection::slug).orElse(null)).get();

        assertThat(seen).isEqualTo("college");
    }
}
