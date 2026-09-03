package com.lms.platform.provisioning;

import com.lms.platform.entity.Tenant;

/** Cloud-specific creation and lifecycle operations for isolated tenant databases. */
public interface TenantDatabaseProvisioner {
    ProvisionedDatabase create(Tenant tenant, String generatedDatabasePassword);
    ProvisioningState state(String providerProjectRef);
    void pause(String providerProjectRef);
    void restore(String providerProjectRef);

    record ProvisionedDatabase(String providerProjectRef, String jdbcUrl, String username) {}
    enum ProvisioningState { READY, PENDING, PAUSED, FAILED }
}
