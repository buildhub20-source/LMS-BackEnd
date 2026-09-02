-- Platform records are separate from tenant LMS data. Each tenant has a
-- different cloud database containing its own lms schema and migrations.
CREATE SCHEMA IF NOT EXISTS platform;

CREATE TABLE IF NOT EXISTS platform.platform_admins (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_platform_admins PRIMARY KEY (id),
    CONSTRAINT uk_platform_admins_email UNIQUE (email),
    CONSTRAINT ck_platform_admins_email_lowercase CHECK (email = lower(email))
);

CREATE TABLE IF NOT EXISTS platform.tenants (
    id                          UUID         NOT NULL DEFAULT gen_random_uuid(),
    name                        VARCHAR(160) NOT NULL,
    slug                        VARCHAR(63)  NOT NULL,
    status                      VARCHAR(32)  NOT NULL,
    provider                    VARCHAR(32)  NOT NULL,
    provider_project_ref        VARCHAR(128),
    jdbc_url                    VARCHAR(1024),
    database_username           VARCHAR(255),
    encrypted_database_password TEXT,
    owner_name                  VARCHAR(100) NOT NULL,
    owner_email                 VARCHAR(255) NOT NULL,
    encrypted_owner_password    TEXT,
    failure_reason              VARCHAR(1000),
    suspended_at                TIMESTAMPTZ,
    deletion_scheduled_at       TIMESTAMPTZ,
    provisioned_at              TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_platform_tenants PRIMARY KEY (id),
    CONSTRAINT uk_platform_tenants_slug UNIQUE (slug),
    CONSTRAINT ck_platform_tenants_slug CHECK (slug ~ '^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$'),
    CONSTRAINT ck_platform_tenants_status CHECK (status IN
        ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'DELETION_SCHEDULED', 'PROVISION_FAILED', 'DELETED'))
);

CREATE INDEX IF NOT EXISTS idx_platform_tenants_status ON platform.tenants(status);

CREATE TABLE IF NOT EXISTS platform.tenant_audit_events (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL,
    actor_id    UUID,
    event_type  VARCHAR(64)  NOT NULL,
    message     VARCHAR(1000),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_platform_tenant_audit_events PRIMARY KEY (id),
    CONSTRAINT fk_platform_tenant_audit_tenant FOREIGN KEY (tenant_id)
        REFERENCES platform.tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_platform_tenant_audit_tenant_created
    ON platform.tenant_audit_events(tenant_id, created_at DESC);
