ALTER TABLE platform.tenants DROP CONSTRAINT IF EXISTS ck_platform_tenants_status;

ALTER TABLE platform.tenants ADD CONSTRAINT ck_platform_tenants_status CHECK (status IN
    ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'CLOUD_PAUSED', 'CLOUD_RESTORING',
     'DELETION_SCHEDULED', 'PROVISION_FAILED', 'DELETED'));
