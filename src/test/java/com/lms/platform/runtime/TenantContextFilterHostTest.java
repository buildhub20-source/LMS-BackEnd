package com.lms.platform.runtime;

import com.lms.config.PlatformConfig;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.service.TenantSecretCipher;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("tenant resolution from the request host")
class TenantContextFilterHostTest {

    private final TenantRepository tenants = mock(TenantRepository.class);
    private final TenantContextFilter filter;

    TenantContextFilterHostTest() {
        PlatformConfig config = new PlatformConfig();
        config.setEnabled(true);
        config.setBaseDomain("gradusmind.com");
        filter = new TenantContextFilter(config, tenants, mock(TenantSecretCipher.class));
        when(tenants.findBySlug(anyString())).thenReturn(Optional.empty());
    }

    private MockHttpServletResponse request(String host) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.setServerName(host);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));
        return response;
    }

    @ParameterizedTest
    @ValueSource(strings = {"platform.gradusmind.com", "app.gradusmind.com", "chat.gradusmind.com", "www.gradusmind.com"})
    @DisplayName("a reserved subdomain is not looked up as an institute")
    void reservedSubdomainHasNoTenant(String host) throws Exception {
        MockHttpServletResponse response = request(host);

        verify(tenants, never()).findBySlug(anyString());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("an institute subdomain is still looked up")
    void instituteSubdomainIsLookedUp() throws Exception {
        MockHttpServletResponse response = request("college.gradusmind.com");

        verify(tenants).findBySlug("college");
        assertThat(response.getStatus()).as("unknown institute").isEqualTo(404);
    }
}
