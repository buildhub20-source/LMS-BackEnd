package com.lms.user.controller;

import com.lms.common.constants.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void profileEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get(ApiPaths.PROFILE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void profileUpdateRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(put(ApiPaths.PROFILE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Name\",\"phone\":\"+1234567890\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
