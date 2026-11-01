package com.lms.platform;

import com.lms.common.exception.ApplicationException;
import com.lms.config.PlatformConfig;
import com.lms.platform.dto.CreateTenantRequest;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantConfig;
import com.lms.platform.entity.TenantStatus;
import com.lms.platform.provisioning.TenantDatabaseInitializer;
import com.lms.platform.provisioning.TenantDatabaseProvisioner;
import com.lms.platform.repository.TenantAuditEventRepository;
import com.lms.platform.repository.TenantConfigRepository;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.service.TenantProvisioningService;
import com.lms.platform.service.TenantSecretCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TenantManagementServiceTest {
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final TenantConfigRepository configs = mock(TenantConfigRepository.class);
    private final TenantAuditEventRepository audits = mock(TenantAuditEventRepository.class);
    private final TenantDatabaseProvisioner provisioner = mock(TenantDatabaseProvisioner.class);
    private final TenantDatabaseInitializer initializer = mock(TenantDatabaseInitializer.class);
    private final TenantSecretCipher cipher = mock(TenantSecretCipher.class);
    private final PlatformConfig platform = new PlatformConfig();
    private TenantProvisioningService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach void setup() {
        service = new TenantProvisioningService(tenants, configs, audits, provisioner, initializer, cipher, platform);
        when(cipher.encrypt(any())).thenReturn("encrypted");
        when(tenants.save(any())).thenAnswer(inv -> { Tenant tenant = inv.getArgument(0); if (tenant.getId() == null) tenant.setId(tenantId); return tenant; });
    }

    private CreateTenantRequest request() {
        return new CreateTenantRequest(" Example Academy ", "example-academy", "Admin", "ADMIN@example.com", "password-for-test", null);
    }

    @Test void registersTenantAndInitialConfig() {
        var result = service.create(request(), UUID.randomUUID());
        assertEquals(TenantStatus.PROVISIONING, result.status());
        assertEquals("example-academy", result.slug());
        assertEquals("admin@example.com", result.ownerEmail());
        var config = ArgumentCaptor.forClass(TenantConfig.class);
        verify(configs).save(config.capture());
        assertEquals(tenantId, config.getValue().getTenantId());
        assertTrue(config.getValue().isLiveClassesEnabled());
        assertNull(config.getValue().getGoogleCalendarEmail());
        assertNull(config.getValue().getGoogleCalendarRefreshToken());
        verify(audits).save(any());
    }

    @Test void rejectsDuplicateSlugWithoutSaving() {
        when(tenants.existsBySlug("example-academy")).thenReturn(true);
        assertThrows(ApplicationException.class, () -> service.create(request(), UUID.randomUUID()));
        verify(tenants, never()).save(any());
        verify(configs, never()).save(any());
    }

    @Test void suspendsActiveTenantAndAudits() {
        Tenant tenant = Tenant.builder().id(tenantId).status(TenantStatus.ACTIVE).build();
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant));
        assertEquals(TenantStatus.SUSPENDED, service.suspend(tenantId, UUID.randomUUID()).status());
        assertNotNull(tenant.getSuspendedAt());
        verify(audits).save(any());
    }

    @Test void refusesToSuspendTenantScheduledForDeletion() {
        Tenant tenant = Tenant.builder().id(tenantId).status(TenantStatus.DELETION_SCHEDULED).build();
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant));
        assertThrows(ApplicationException.class, () -> service.suspend(tenantId, UUID.randomUUID()));
        verify(tenants, never()).save(any());
    }

    @Test void reportsSupabaseForbiddenAsProvisioningFailure() {
        Tenant tenant = Tenant.builder().id(tenantId).status(TenantStatus.PROVISIONING).build();
        when(tenants.findByIdForProvisioning(tenantId)).thenReturn(Optional.of(tenant));
        when(provisioner.create(any(), any())).thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatus.FORBIDDEN, "Forbidden", org.springframework.http.HttpHeaders.EMPTY,
                new byte[0], java.nio.charset.StandardCharsets.UTF_8));
        var result = service.progress(tenantId, UUID.randomUUID());
        assertEquals(TenantStatus.PROVISION_FAILED, result.status());
        assertTrue(result.failureReason().contains("HTTP 403"));
        verify(audits).save(any());
    }
}
