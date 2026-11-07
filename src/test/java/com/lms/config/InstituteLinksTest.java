package com.lms.config;

import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import com.lms.platform.runtime.TenantContextTaskDecorator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("links in mail open on the institute's own address")
class InstituteLinksTest {

    private static TenantConnection institute(String slug) {
        return new TenantConnection(UUID.randomUUID(), slug, "Test Institute",
                "jdbc:postgresql://db.invalid/postgres", "user", "secret");
    }

    private static AppProperties properties(String frontendBaseUrl, String tenantBaseDomain) {
        AppProperties properties = new AppProperties();
        properties.setFrontendBaseUrl(frontendBaseUrl);
        properties.setTenantBaseDomain(tenantBaseDomain);
        return properties;
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("inside an institute, links use <slug>.<domain>")
    void instituteLinks() {
        AppProperties links = properties("https://platform.gradusmind.com", "gradusmind.com");
        TenantContext.set(institute("college"));

        assertThat(links.invitationLink("t1")).isEqualTo("https://college.gradusmind.com/auth/accept-invitation?token=t1");
        assertThat(links.passwordResetLink("t2")).isEqualTo("https://college.gradusmind.com/auth/reset-password?token=t2");
        assertThat(links.liveClassesLink()).isEqualTo("https://college.gradusmind.com/learn/live-classes");
    }

    @Test
    @DisplayName("outside an institute, links stay on the platform address")
    void platformLinks() {
        AppProperties links = properties("https://platform.gradusmind.com/", "gradusmind.com");

        assertThat(links.invitationLink("t1")).isEqualTo("https://platform.gradusmind.com/auth/accept-invitation?token=t1");
    }

    @Test
    @DisplayName("without a base domain (local development) nothing changes")
    void noBaseDomain() {
        AppProperties links = properties("http://localhost:3000", "");
        TenantContext.set(institute("college"));

        assertThat(links.liveClassesLink()).isEqualTo("http://localhost:3000/learn/live-classes");
    }

    @Test
    @DisplayName("the port of the base URL is kept")
    void keepsPort() {
        assertThat(AppProperties.instituteUrl("http://localhost:3000", "lms", "localhost"))
                .isEqualTo("http://lms.localhost:3000");
    }

    @Test
    @DisplayName("background tasks keep the institute, and the worker is left clean")
    void decoratorCarriesInstitute() throws Exception {
        TenantContextTaskDecorator decorator = new TenantContextTaskDecorator();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            TenantContext.set(institute("college"));
            AtomicReference<String> seen = new AtomicReference<>();
            CompletableFuture.runAsync(decorator.decorate(
                    () -> seen.set(TenantContext.current().map(TenantConnection::slug).orElse(null))), worker).get();
            TenantContext.clear();

            assertThat(seen.get()).isEqualTo("college");
            String leftover = CompletableFuture.supplyAsync(
                    () -> TenantContext.current().map(TenantConnection::slug).orElse(null), worker).get();
            assertThat(leftover).as("the worker thread must not keep the institute").isNull();
        } finally {
            worker.shutdownNow();
        }
    }
}
