-- Platform Tenant Configurations and Global Broadcast Announcements

CREATE SCHEMA IF NOT EXISTS platform;

CREATE TABLE IF NOT EXISTS platform.tenant_configs (
    id                          UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id                   UUID        NOT NULL,
    max_users                   INT         NOT NULL DEFAULT 500,
    max_courses                 INT         NOT NULL DEFAULT 50,
    max_storage_gb              INT         NOT NULL DEFAULT 20,
    ai_features_enabled         BOOLEAN     NOT NULL DEFAULT TRUE,
    advanced_analytics_enabled  BOOLEAN     NOT NULL DEFAULT TRUE,
    custom_certificates_enabled BOOLEAN     NOT NULL DEFAULT TRUE,
    code_evaluator_enabled      BOOLEAN     NOT NULL DEFAULT TRUE,
    live_proctoring_enabled     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_platform_tenant_configs PRIMARY KEY (id),
    CONSTRAINT uk_platform_tenant_configs_tenant_id UNIQUE (tenant_id),
    CONSTRAINT fk_platform_tenant_configs_tenant FOREIGN KEY (tenant_id)
        REFERENCES platform.tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_platform_tenant_configs_tenant ON platform.tenant_configs(tenant_id);

CREATE TABLE IF NOT EXISTS platform.broadcast_announcements (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    message     TEXT         NOT NULL,
    type        VARCHAR(32)  NOT NULL DEFAULT 'INFO',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    starts_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ,
    created_by  UUID,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_platform_broadcast_announcements PRIMARY KEY (id),
    CONSTRAINT ck_platform_broadcast_announcements_type CHECK (type IN ('INFO', 'WARNING', 'CRITICAL', 'MAINTENANCE'))
);

CREATE INDEX IF NOT EXISTS idx_platform_announcements_active ON platform.broadcast_announcements(active, starts_at);
