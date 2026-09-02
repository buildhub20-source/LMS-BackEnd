package com.lms.platform.service;

import com.lms.config.PlatformConfig;
import com.lms.platform.entity.TenantStatus;
import com.lms.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs outside the provisioning service so every deferred provisioning attempt
 * crosses Spring's transactional proxy. The row lock in progress(...) then
 * serializes scheduler and global-admin retries for the same tenant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantProvisioningScheduler {
    private final PlatformConfig platformConfig;
    private final TenantRepository tenantRepository;
    private final TenantProvisioningService provisioningService;

    @Scheduled(fixedDelayString = "${TENANT_PROVISIONING_POLL_MS:30000}")
    public void progressPendingTenants() {
        if (!platformConfig.isEnabled()) {
            return;
        }
        tenantRepository.findByStatus(TenantStatus.PROVISIONING).forEach(tenant -> {
            try {
                provisioningService.progress(tenant.getId(), null);
            } catch (Exception ex) {
                log.debug("Deferred tenant provisioning retry for {}", tenant.getId());
            }
        });
        tenantRepository.findByStatus(TenantStatus.CLOUD_PAUSING).forEach(tenant -> {
            try {
                provisioningService.refreshCloudPause(tenant.getId());
            } catch (Exception ex) {
                log.debug("Deferred cloud pause check for {}", tenant.getId());
            }
        });
        tenantRepository.findByStatus(TenantStatus.CLOUD_RESTORING).forEach(tenant -> {
            try {
                provisioningService.refreshCloudRestoration(tenant.getId());
            } catch (Exception ex) {
                log.debug("Deferred cloud restore check for {}", tenant.getId());
            }
        });
    }
}
