package com.lms.security;

import com.lms.config.JwtConfig;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.security.jwt.JwtAuthenticationFilter;
import com.lms.security.jwt.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** A token issued inside one institute must never authenticate a request to another. */
class CrossTenantTokenTest {

    private static final TenantConnection TENANT_A = tenant("college-a");
    private static final TenantConnection TENANT_B = tenant("college-b");

    private final JwtService jwtService = new JwtService(config());
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
    private final LmsUserDetails admin = new LmsUserDetails(UUID.randomUUID(), "admin@college-a.edu", "Admin",
            null, true, false, Set.of("ADMIN"), Set.of("USER_VIEW"), null);

    @AfterEach
    void reset() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void aTokenWorksInTheTenantThatIssuedIt() throws Exception {
        String token = issueIn(TENANT_A);

        assertThat(authenticate(token, TENANT_A)).isNotNull();
    }

    @Test
    void aTokenFromTenantAIsRejectedOnTenantB() throws Exception {
        String token = issueIn(TENANT_A);

        assertThat(authenticate(token, TENANT_B)).isNull();
    }

    @Test
    void aTenantTokenIsRejectedWhenNoTenantIsSelected() throws Exception {
        String token = issueIn(TENANT_A);

        assertThat(authenticate(token, null)).isNull();
    }

    private String issueIn(TenantConnection tenant) {
        TenantContext.set(tenant);
        try {
            return jwtService.generateAccessToken(admin, UUID.randomUUID());
        } finally {
            TenantContext.clear();
        }
    }

    private Authentication authenticate(String token, TenantConnection requestTenant) throws Exception {
        SecurityContextHolder.clearContext();
        if (requestTenant != null) TenantContext.set(requestTenant);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static TenantConnection tenant(String slug) {
        return new TenantConnection(UUID.randomUUID(), slug, slug, "jdbc:h2:mem:" + slug, "sa", "");
    }

    private static JwtConfig config() {
        JwtConfig config = new JwtConfig();
        config.setSecret("unit-test-secret-key-that-is-long-enough-1234");
        config.setIssuer("lms-backend");
        config.setAccessTokenTtl(Duration.ofMinutes(15));
        config.setRefreshTokenTtl(Duration.ofDays(7));
        config.setClockSkew(Duration.ofSeconds(30));
        return config;
    }
}
