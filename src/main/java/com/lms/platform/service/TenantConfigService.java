package com.lms.platform.service;

import com.lms.common.exception.ResourceNotFoundException;
import com.lms.platform.dto.TenantConfigDto;
import com.lms.platform.dto.UpdateTenantConfigRequest;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantAuditEvent;
import com.lms.platform.entity.TenantConfig;
import com.lms.platform.repository.TenantAuditEventRepository;
import com.lms.platform.repository.TenantConfigRepository;
import com.lms.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantConfigService {

    private final TenantRepository tenantRepository;
    private final TenantConfigRepository configRepository;
    private final TenantAuditEventRepository auditRepository;

    @Transactional
    public TenantConfigDto getConfig(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));

        TenantConfig config = configRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> configRepository.save(TenantConfig.builder()
                        .tenantId(tenant.getId())
                        .maxUsers(500)
                        .maxCourses(50)
                        .maxStorageGb(20)
                        .aiFeaturesEnabled(true)
                        .advancedAnalyticsEnabled(true)
                        .customCertificatesEnabled(true)
                        .codeEvaluatorEnabled(true)
                        .liveProctoringEnabled(false)
                        .build()));

        return TenantConfigDto.from(config);
    }

    @Transactional
    public TenantConfigDto updateConfig(UUID tenantId, UpdateTenantConfigRequest request, UUID actorId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));

        TenantConfig config = configRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> TenantConfig.builder().tenantId(tenant.getId()).build());

        config.setMaxUsers(request.maxUsers());
        config.setMaxCourses(request.maxCourses());
        config.setMaxStorageGb(request.maxStorageGb());
        config.setAiFeaturesEnabled(request.aiFeaturesEnabled());
        config.setAdvancedAnalyticsEnabled(request.advancedAnalyticsEnabled());
        config.setCustomCertificatesEnabled(request.customCertificatesEnabled());
        config.setCodeEvaluatorEnabled(request.codeEvaluatorEnabled());
        config.setLiveProctoringEnabled(request.liveProctoringEnabled());

        TenantConfig saved = configRepository.save(config);

        auditRepository.save(TenantAuditEvent.builder()
                .tenantId(tenant.getId())
                .actorId(actorId)
                .eventType("TENANT_CONFIG_UPDATED")
                .message("Updated feature flags & quotas: maxUsers=" + request.maxUsers()
                        + ", maxCourses=" + request.maxCourses()
                        + ", AI=" + request.aiFeaturesEnabled()
                        + ", Proctoring=" + request.liveProctoringEnabled())
                .createdAt(Instant.now())
                .build());

        log.info("Platform admin updated configuration for tenant {}", tenant.getSlug());
        return TenantConfigDto.from(saved);
    }
}
