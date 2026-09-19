-- Add chat_file_retention_days to platform.tenant_configs (default: 30 days)
ALTER TABLE platform.tenant_configs
    ADD COLUMN IF NOT EXISTS chat_file_retention_days INT NOT NULL DEFAULT 30;
