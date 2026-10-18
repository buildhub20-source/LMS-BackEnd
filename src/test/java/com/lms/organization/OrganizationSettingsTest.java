package com.lms.organization;

import com.lms.organization.repository.OrganizationSettingsRepository;
import com.lms.organization.service.OrganizationSettingsService;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizationSettingsTest {

    private static final String URL = "/api/v1/organization";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationSettingsService service;

    @Autowired
    private OrganizationSettingsRepository repository;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void anyoneCanReadBrandingAndItIsNeverSampleData() throws Exception {
        repository.deleteAll();

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryColor").value("#10b981"))
                .andExpect(content().string(not(containsString("Acme"))))
                .andExpect(content().string(not(containsString("acme"))));
    }

    @Test
    void anUnsavedTenantDefaultsToItsOwnName() {
        repository.deleteAll();
        TenantContext.set(new TenantConnection(UUID.randomUUID(), "srp", "Sri Ramana Polytechnic",
                "jdbc:h2:mem:unused", "sa", ""));

        assertThat(service.current().getName()).isEqualTo("Sri Ramana Polytechnic");
    }

    @Test
    void anonymousCallersCannotChangeSettings() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hijacked\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void nonAdminsCannotChangeSettings() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hijacked\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAdminUpdatesOnlyTheFieldsSent() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sri Ramana Polytechnic\",\"supportEmail\":\"help@srp.edu.in\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"primaryColor\":\"#3b82f6\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Sri Ramana Polytechnic"))
                .andExpect(jsonPath("$.data.supportEmail").value("help@srp.edu.in"))
                .andExpect(jsonPath("$.data.primaryColor").value("#3b82f6"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidValuesAreRejectedWithFieldErrors() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supportEmail\":\"not-an-email\",\"primaryColor\":\"green\","
                                + "\"logoUrl\":\"javascript:alert(1)\",\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(4));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unknownFieldsAreRejected() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"customDomain\":\"evil.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
