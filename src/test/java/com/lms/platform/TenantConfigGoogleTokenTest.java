package com.lms.platform;

import com.lms.platform.dto.UpdateTenantConfigRequest;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantConfig;
import com.lms.platform.entity.TenantStatus;
import com.lms.platform.repository.TenantAuditEventRepository;
import com.lms.platform.repository.TenantConfigRepository;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.service.TenantConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Google Calendar connection must survive a platform admin editing the
 * tenant's configuration.
 *
 * <p>Written while investigating a tenant that completed Google consent and
 * was later told to "Connect an organization Google Calendar" when scheduling
 * a Meet class. The refresh token is written by one raw UPDATE and read back
 * on the issuing path, while this service rewrites the whole config row — so
 * if the update dropped the token, switching the provider to Google Meet would
 * itself undo the connection that makes Google Meet usable.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("tenant config updates and the Google connection")
class TenantConfigGoogleTokenTest {

    @Mock private TenantRepository tenants;
    @Mock private TenantConfigRepository configs;
    @Mock private TenantAuditEventRepository audits;

    @InjectMocks private TenantConfigService service;

    private final UUID tenantId = UUID.randomUUID();
    private TenantConfig existing;

    @BeforeEach
    void setUp() {
        Tenant tenant = Tenant.builder()
                .id(tenantId).slug("lms").name("LMS").status(TenantStatus.ACTIVE).build();
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant));

        existing = TenantConfig.builder()
                .tenantId(tenantId)
                .liveClassesEnabled(true)
                .liveSessionProvider("LIVEKIT")
                .googleCalendarRefreshToken("encrypted-refresh-token")
                .googleCalendarEmail("buildhub20@gmail.com")
                .build();
        when(configs.findByTenantId(tenantId)).thenReturn(Optional.of(existing));
        when(configs.save(any(TenantConfig.class))).thenAnswer(call -> call.getArgument(0));
    }

    private UpdateTenantConfigRequest request(String provider) {
        return new UpdateTenantConfigRequest(
                100, 50, 10,
                false, false, false, false, false,
                null,
                true, provider,
                null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private TenantConfig captureSaved() {
        ArgumentCaptor<TenantConfig> captor = ArgumentCaptor.forClass(TenantConfig.class);
        verify(configs).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("switching the provider to Google Meet keeps the connection intact")
    void switchingToGoogleMeetKeepsTheToken() {
        service.updateConfig(tenantId, request("GOOGLE_MEET"), UUID.randomUUID());

        TenantConfig saved = captureSaved();
        assertThat(saved.getLiveSessionProvider()).isEqualTo("GOOGLE_MEET");
        // The whole point: enabling Meet must not undo the Google connection
        // that makes Meet work.
        assertThat(saved.getGoogleCalendarRefreshToken()).isEqualTo("encrypted-refresh-token");
        assertThat(saved.getGoogleCalendarEmail()).isEqualTo("buildhub20@gmail.com");
    }

    @Test
    @DisplayName("an unrelated limit change leaves the connection alone")
    void unrelatedEditKeepsTheToken() {
        service.updateConfig(tenantId, request(null), UUID.randomUUID());

        assertThat(captureSaved().getGoogleCalendarRefreshToken()).isEqualTo("encrypted-refresh-token");
    }

    @Test
    @DisplayName("repeated edits do not erode the connection")
    void repeatedEditsKeepTheToken() {
        // The tenant in question was edited seven times in a row before the
        // failure, so once is not a sufficient check.
        for (int i = 0; i < 7; i++) {
            service.updateConfig(tenantId, request("GOOGLE_MEET"), UUID.randomUUID());
        }
        assertThat(existing.getGoogleCalendarRefreshToken()).isEqualTo("encrypted-refresh-token");
    }
}
