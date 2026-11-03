package com.lms.live.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoogleCalendarControllerTest {
    @Test void redirectsToTheAuthorizedTenantOnTheConfiguredFrontend() {
        assertEquals("http://lms.localhost:3000", GoogleCalendarController.tenantFrontendUrl("http://localhost:3000", "localhost", "lms"));
        assertEquals("https://school.example.com", GoogleCalendarController.tenantFrontendUrl("https://example.com", "example.com", "school"));
        assertThrows(IllegalArgumentException.class, () -> GoogleCalendarController.tenantFrontendUrl("https://evil.example", "example.com", "school"));
        assertThrows(IllegalArgumentException.class, () -> GoogleCalendarController.tenantFrontendUrl("http://localhost:3000", "localhost", "evil.example"));
    }
}
