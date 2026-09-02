package com.lms.platform.entity;

/** Lifecycle state controlled only by a global platform administrator. */
public enum TenantStatus {
    PROVISIONING,
    ACTIVE,
    SUSPENDED,
    CLOUD_PAUSING,
    CLOUD_PAUSED,
    CLOUD_RESTORING,
    DELETION_SCHEDULED,
    PROVISION_FAILED,
    DELETED
}
