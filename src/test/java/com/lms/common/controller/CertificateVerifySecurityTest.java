package com.lms.common.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CertificateVerifySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anyoneMayReachTheVerifyEndpoint() throws Exception {
        // The certificate service is disabled in tests, so getting past security yields 503, not 401.
        mockMvc.perform(get("/api/v1/certificates/verify/CERT-2026-AB12CD34"))
                .andExpect(status().isServiceUnavailable());
    }
}
