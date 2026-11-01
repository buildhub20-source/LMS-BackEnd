package com.lms.live.controller;

import com.lms.live.dto.CreateLiveSessionRequest;
import com.lms.live.service.LiveSessionService;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import com.lms.common.exception.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class LiveSessionCreatePermissionTest {
    @Autowired LiveSessionController controller;
    @MockBean LiveSessionService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); TenantContext.clear(); }

    private void signIn(Set<String> roles, Set<String> permissions) {
        TenantContext.set(new TenantConnection(UUID.randomUUID(), "school", "School", null, null, null));
        var principal = LmsUserDetails.fromClaims(userId, "teacher@example.test", roles, permissions, UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private CreateLiveSessionRequest request() {
        var start = Instant.now().plusSeconds(3600);
        return new CreateLiveSessionRequest("Class", null, start, start.plusSeconds(3600), "google_meet");
    }

    @Test void instructorWithoutCreatePermissionIsForbidden() {
        signIn(Set.of("INSTRUCTOR"), Set.of("LIVE_SESSION_MANAGE"));
        assertThatThrownBy(() -> controller.scheduleSession(courseId, request())).isInstanceOf(AccessDeniedException.class);
        verify(service, never()).createSession(any(), any(), any());
    }

    @Test void instructorWithCreatePermissionCanSchedule() {
        signIn(Set.of("INSTRUCTOR"), Set.of("LIVE_SESSION_CREATE"));
        var request = request();
        controller.scheduleSession(courseId, request);
        verify(service).createSession(courseId, request, userId);
    }

    @Test void administratorCanScheduleWithoutExplicitPermission() {
        signIn(Set.of("ADMIN"), Set.of());
        var request = request();
        controller.scheduleSession(courseId, request);
        verify(service).createSession(courseId, request, userId);
    }

    @Test void googleMeetCannotUseAnotherTenantWhenContextIsMissing() {
        signIn(Set.of("ADMIN"), Set.of());
        TenantContext.clear();
        assertThatThrownBy(() -> controller.scheduleSession(courseId, request()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("organization");
        verify(service, never()).createSession(any(), any(), any());
    }
}
