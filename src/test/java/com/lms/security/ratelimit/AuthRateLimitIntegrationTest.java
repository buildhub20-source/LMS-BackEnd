package com.lms.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "lms.security.rate-limit.enabled=true",
        "lms.security.rate-limit.login=3",
        "lms.security.rate-limit.forgot-password=2"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRateLimitIntegrationTest {

    private static final String BAD_LOGIN = "{\"email\":\"nobody@example.com\",\"password\":\"wrong-password\"}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void repeatedLoginsFromOneAddressAreThrottled() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(login("203.0.113.10")).andExpect(status().is(not(429)));
        }

        mockMvc.perform(login("203.0.113.10"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void anotherAddressIsNotAffected() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(login("203.0.113.20"));
        }

        mockMvc.perform(login("203.0.113.21")).andExpect(status().is(not(429)));
    }

    @Test
    void passwordResetEmailsHaveTheirOwnTighterLimit() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(forgotPassword("203.0.113.30")).andExpect(status().is(not(429)));
        }

        mockMvc.perform(forgotPassword("203.0.113.30")).andExpect(status().isTooManyRequests());
        mockMvc.perform(login("203.0.113.30")).andExpect(status().is(not(429)));
    }

    private MockHttpServletRequestBuilder login(String ip) {
        return post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(BAD_LOGIN);
    }

    private MockHttpServletRequestBuilder forgotPassword(String ip) {
        return post("/api/v1/auth/forgot-password")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\"}");
    }
}
