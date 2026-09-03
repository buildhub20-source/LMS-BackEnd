package com.lms.platform.service;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.config.PlatformConfig;
import com.lms.platform.dto.CreateTenantRequest;
import com.lms.platform.dto.TenantResponse;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantAuditEvent;
import com.lms.platform.entity.TenantStatus;
import com.lms.platform.provisioning.TenantDatabaseInitializer;
import com.lms.platform.provisioning.TenantDatabaseProvisioner;
import com.lms.platform.repository.TenantAuditEventRepository;
import com.lms.platform.repository.TenantRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantProvisioningService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final TenantAuditEventRepository auditRepository;
    private final TenantDatabaseProvisioner provisioner;
    private final TenantDatabaseInitializer initializer;
    private final TenantSecretCipher cipher;
    private final PlatformConfig config;

    public TenantProvisioningService(TenantRepository tenantRepository,
                                     TenantAuditEventRepository auditRepository,
                                     TenantDatabaseProvisioner provisioner,
                                     TenantDatabaseInitializer initializer,
                                     TenantSecretCipher cipher,
                                     PlatformConfig config) {
        this.tenantRepository = tenantRepository;
        this.auditRepository = auditRepository;
        this.provisioner = provisioner;
        this.initializer = initializer;
        this.cipher = cipher;
        this.config = config;
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request, UUID actorId) {
        String slug = request.slug().trim().toLowerCase();
        if (tenantRepository.existsBySlug(slug)) {
            throw new ApplicationException(ErrorCode.RESOURCE_ALREADY_EXISTS, "A tenant already uses this slug");
        }
        Tenant tenant = Tenant.builder()
                .name(request.name().trim())
                .slug(slug)
                .status(TenantStatus.PROVISIONING)
                .provider(config.getProvisioning().getProvider().trim().toUpperCase())
                .ownerName(request.ownerName().trim())
                .ownerEmail(request.ownerEmail().trim().toLowerCase())
                .encryptedOwnerPassword(cipher.encrypt(request.initialAdminPassword()))
                .build();
        tenantRepository.save(tenant);
        audit(tenant, actorId, "TENANT_REQUESTED", "Tenant database provisioning requested");
        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse progress(UUID tenantId, UUID actorId) {
        Tenant tenant = tenantRepository.findByIdForProvisioning(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));
        if (tenant.getStatus() == TenantStatus.ACTIVE) {
            return TenantResponse.from(tenant);
        }
        if (tenant.getStatus() == TenantStatus.SUSPENDED || tenant.getStatus() == TenantStatus.CLOUD_PAUSING || tenant.getStatus() == TenantStatus.CLOUD_PAUSED
                || tenant.getStatus() == TenantStatus.CLOUD_RESTORING || tenant.getStatus() == TenantStatus.DELETION_SCHEDULED
                || tenant.getStatus() == TenantStatus.DELETED) {
            throw new ApplicationException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "This tenant cannot be provisioned in its current state");
        }
        try {
            if (tenant.getProviderProjectRef() == null) {
                String databasePassword = randomSecret();
                TenantDatabaseProvisioner.ProvisionedDatabase database = provisioner.create(tenant, databasePassword);
                tenant.setProviderProjectRef(database.providerProjectRef());
                tenant.setJdbcUrl(database.jdbcUrl());
                tenant.setDatabaseUsername(database.username());
                tenant.setEncryptedDatabasePassword(cipher.encrypt(databasePassword));
                tenant.setStatus(TenantStatus.PROVISIONING);
                tenant.setFailureReason(null);
                audit(tenant, actorId, "DATABASE_CREATED", "Cloud tenant database requested");
            }

            TenantDatabaseProvisioner.ProvisioningState state = provisioner.state(tenant.getProviderProjectRef());
            if (state != TenantDatabaseProvisioner.ProvisioningState.READY
                    && state != TenantDatabaseProvisioner.ProvisioningState.FAILED) {
                tenantRepository.save(tenant);
                return TenantResponse.from(tenant);
            }
            if (state == TenantDatabaseProvisioner.ProvisioningState.FAILED) {
                fail(tenant, actorId, "Cloud provider reported tenant database provisioning failure");
                return TenantResponse.from(tenant);
            }

            initializer.initialize(tenant, cipher.decrypt(tenant.getEncryptedDatabasePassword()),
                    cipher.decrypt(tenant.getEncryptedOwnerPassword()));
            tenant.setEncryptedOwnerPassword(null);
            tenant.setStatus(TenantStatus.ACTIVE);
            tenant.setProvisionedAt(Instant.now());
            tenant.setFailureReason(null);
            audit(tenant, actorId, "TENANT_ACTIVATED", "LMS migrations and tenant administrator initialized");
            tenantRepository.save(tenant);
            return TenantResponse.from(tenant);
        } catch (ApplicationException ex) {
            fail(tenant, actorId, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.warn("Tenant {} provisioning did not complete", tenant.getId(), ex);
            fail(tenant, actorId, "Tenant provisioning did not complete; retry provisioning after correcting cloud configuration");
            return TenantResponse.from(tenant);
        }
    }

    @Transactional
    public TenantResponse suspend(UUID tenantId, UUID actorId) {
        Tenant tenant = get(tenantId);
        if (tenant.getStatus() == TenantStatus.DELETED || tenant.getStatus() == TenantStatus.DELETION_SCHEDULED) {
            throw new ApplicationException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant is being deleted");
        }
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenant.setSuspendedAt(Instant.now());
        audit(tenant, actorId, "TENANT_SUSPENDED", "Tenant access suspended by global administrator");
        tenantRepository.save(tenant);
        return TenantResponse.from(tenant);
    }

    /** Pauses the actual provider project, preserving data while releasing its active-project slot. */
    @Transactional
    public TenantResponse pauseCloudProject(UUID tenantId, UUID actorId) {
        Tenant tenant = getForProvisioning(tenantId);
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Only an active tenant cloud project can be paused");
        }
        provisioner.pause(tenant.getProviderProjectRef());
        tenant.setStatus(TenantStatus.CLOUD_PAUSING);
        audit(tenant, actorId, "CLOUD_PROJECT_PAUSE_REQUESTED", "Supabase tenant project pause requested by global administrator");
        tenantRepository.save(tenant);
        return TenantResponse.from(tenant);
    }

    @Transactional
    public void refreshCloudPause(UUID tenantId) {
        Tenant tenant = getForProvisioning(tenantId);
        if (tenant.getStatus() != TenantStatus.CLOUD_PAUSING) {
            return;
        }
        TenantDatabaseProvisioner.ProvisioningState state = provisioner.state(tenant.getProviderProjectRef());
        if (state == TenantDatabaseProvisioner.ProvisioningState.PAUSED) {
            tenant.setStatus(TenantStatus.CLOUD_PAUSED);
            audit(tenant, null, "CLOUD_PROJECT_PAUSED", "Supabase tenant project pause completed");
            tenantRepository.save(tenant);
        } else if (state == TenantDatabaseProvisioner.ProvisioningState.FAILED) {
            tenant.setStatus(TenantStatus.ACTIVE);
            tenant.setFailureReason("Supabase did not complete the cloud pause request");
            audit(tenant, null, "CLOUD_PROJECT_PAUSE_FAILED", tenant.getFailureReason());
            tenantRepository.save(tenant);
        }
    }

    /** Starts a provider restore; the scheduler marks the tenant active only after the database is ready. */
    @Transactional
    public TenantResponse restoreCloudProject(UUID tenantId, UUID actorId) {
        Tenant tenant = getForProvisioning(tenantId);
        if (tenant.getStatus() != TenantStatus.CLOUD_PAUSED) {
            throw new ApplicationException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Only a cloud-paused tenant can be restored");
        }
        provisioner.restore(tenant.getProviderProjectRef());
        tenant.setStatus(TenantStatus.CLOUD_RESTORING);
        audit(tenant, actorId, "CLOUD_PROJECT_RESTORE_REQUESTED", "Supabase tenant project restore requested");
        tenantRepository.save(tenant);
        return TenantResponse.from(tenant);
    }

    @Transactional
    public void refreshCloudRestoration(UUID tenantId) {
        Tenant tenant = getForProvisioning(tenantId);
        if (tenant.getStatus() != TenantStatus.CLOUD_RESTORING) {
            return;
        }
        TenantDatabaseProvisioner.ProvisioningState state = provisioner.state(tenant.getProviderProjectRef());
        if (state == TenantDatabaseProvisioner.ProvisioningState.READY) {
            tenant.setStatus(TenantStatus.ACTIVE);
            audit(tenant, null, "CLOUD_PROJECT_RESTORED", "Supabase tenant project is ready");
            tenantRepository.save(tenant);
        } else if (state == TenantDatabaseProvisioner.ProvisioningState.FAILED) {
            tenant.setStatus(TenantStatus.CLOUD_PAUSED);
            audit(tenant, null, "CLOUD_PROJECT_RESTORE_FAILED", "Supabase restore did not complete; retry restore from platform admin");
            tenantRepository.save(tenant);
        }
    }

    @Transactional
    public TenantResponse scheduleDeletion(UUID tenantId, UUID actorId) {
        Tenant tenant = get(tenantId);
        tenant.setStatus(TenantStatus.DELETION_SCHEDULED);
        tenant.setDeletionScheduledAt(Instant.now().plusSeconds(30L * 24 * 60 * 60));
        audit(tenant, actorId, "TENANT_DELETION_SCHEDULED", "Permanent database purge eligible after 30-day retention");
        tenantRepository.save(tenant);
        return TenantResponse.from(tenant);
    }

    public TenantResponse getResponse(UUID tenantId) { return TenantResponse.from(get(tenantId)); }

    public List<TenantResponse> list() {
        return tenantRepository.findAll().stream()
                .sorted(Comparator.comparing(Tenant::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(TenantResponse::from).toList();
    }

    private Tenant get(UUID tenantId) {
        return tenantRepository.findById(tenantId).orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));
    }

    private Tenant getForProvisioning(UUID tenantId) {
        return tenantRepository.findByIdForProvisioning(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));
    }

    private void fail(Tenant tenant, UUID actorId, String reason) {
        tenant.setStatus(TenantStatus.PROVISION_FAILED);
        tenant.setFailureReason(reason.length() > 1000 ? reason.substring(0, 1000) : reason);
        tenantRepository.save(tenant);
        audit(tenant, actorId, "TENANT_PROVISION_FAILED", tenant.getFailureReason());
    }

    private void audit(Tenant tenant, UUID actorId, String event, String message) {
        auditRepository.save(TenantAuditEvent.builder().tenantId(tenant.getId()).actorId(actorId)
                .eventType(event).message(message).createdAt(Instant.now()).build());
    }

    private String randomSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
