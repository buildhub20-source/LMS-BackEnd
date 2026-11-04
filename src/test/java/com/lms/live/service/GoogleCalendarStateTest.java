package com.lms.live.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.common.exception.BusinessRuleException;
import com.lms.config.PlatformConfig;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The Google Calendar connect link must only work for the admin who asked for it,
 * in the institute it was asked in. Nothing here calls Google: every refusal
 * happens before the code is exchanged.
 */
@DisplayName("Google Calendar connect state")
class GoogleCalendarStateTest {

    private final TenantRepository tenants = mock(TenantRepository.class);
    private GoogleCalendarService service;
    private final UUID admin = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PlatformConfig config = new PlatformConfig();
        config.getGoogleCalendar().setClientId("client-id");
        config.getGoogleCalendar().setClientSecret("client-secret-used-to-sign-state");
        config.getGoogleCalendar().setRedirectUri("http://localhost:8080/api/v1/live-sessions/google/callback");
        service = new GoogleCalendarService(config, tenants, null, null, new ObjectMapper());
        bindTenant("school");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void bindTenant(String slug) {
        TenantContext.set(new TenantConnection(UUID.randomUUID(), slug, slug, "jdbc:none", "u", "p"));
    }

    private String stateFromConnectLink(UUID adminId) {
        String url = service.authorizationUrl(adminId);
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("state");
    }

    @Test
    @DisplayName("the connect link carries the institute and the admin who asked")
    void stateNamesTenantAndAdmin() {
        GoogleCalendarService.State state = service.parseState(stateFromConnectLink(admin));
        assertThat(state.slug()).isEqualTo("school");
        assertThat(state.userId()).isEqualTo(admin);
        assertThat(service.tenantSlugFromState(stateFromConnectLink(admin))).isEqualTo("school");
    }

    @Test
    @DisplayName("another signed-in admin cannot finish someone else's connection")
    void otherAdminRefused() {
        String state = stateFromConnectLink(admin);
        assertThatThrownBy(() -> service.completeConnection("code", state, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different account");
        verifyNoInteractions(tenants);
    }

    @Test
    @DisplayName("a link issued in one institute cannot be finished in another")
    void otherTenantRefused() {
        String state = stateFromConnectLink(admin);
        bindTenant("other-school");
        assertThatThrownBy(() -> service.completeConnection("code", state, admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different account");
        verifyNoInteractions(tenants);
    }

    @Test
    @DisplayName("a tampered or expired state is refused")
    void tamperedOrExpiredRefused() {
        String state = stateFromConnectLink(admin);
        String tampered = "x" + state.substring(1);
        assertThatThrownBy(() -> service.parseState(tampered)).isInstanceOf(BusinessRuleException.class);

        String expired = service.signedState("school|" + (Instant.now().getEpochSecond() - 1) + "|" + admin + "|n");
        assertThatThrownBy(() -> service.parseState(expired)).isInstanceOf(BusinessRuleException.class);

        // The pre-fix format (no admin id) is no longer accepted.
        String oldFormat = service.signedState("school|" + (Instant.now().getEpochSecond() + 600) + "|n");
        assertThatThrownBy(() -> service.parseState(oldFormat)).isInstanceOf(BusinessRuleException.class);
    }
}
