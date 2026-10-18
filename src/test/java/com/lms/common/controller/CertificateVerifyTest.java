package com.lms.common.controller;

import com.lms.config.CertificateServiceConfig;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CertificateVerifyTest {

    private MockRestServiceServer certService;
    private CertificateProxyController controller;

    @BeforeEach
    void setUp() {
        CertificateServiceConfig config = new CertificateServiceConfig();
        config.setBaseUrl("http://cert.test");
        config.setEnabled(true);
        RestTemplate restTemplate = new RestTemplate();
        certService = MockRestServiceServer.bindTo(restTemplate).build();
        controller = new CertificateProxyController(config, restTemplate);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void verifiesThroughTheTenantsPublicEndpointWithoutCredentials() {
        TenantContext.set(new TenantConnection(UUID.randomUUID(), "srp", "SRP", "jdbc:h2:mem:x", "sa", ""));
        certService.expect(requestTo("http://cert.test/api/v1/public/tenants/srp/verify/CERT-2026-AB12CD34"))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess("{\"valid\":true}", MediaType.APPLICATION_JSON));

        var response = controller.verify("CERT-2026-AB12CD34");

        assertThat(new String(response.getBody())).contains("\"valid\":true");
        certService.verify();
    }

    @Test
    void rejectsAMalformedSerialWithoutCallingTheService() {
        assertThatThrownBy(() -> controller.verify("..%2F..%2Finternal"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Certificate not found");
        certService.verify();
    }
}
