ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS live_session_provider VARCHAR(24) NOT NULL DEFAULT 'LIVEKIT';
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS google_calendar_refresh_token TEXT;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS google_calendar_email VARCHAR(255);
ALTER TABLE platform.tenants ADD COLUMN IF NOT EXISTS live_session_provider VARCHAR(24) NOT NULL DEFAULT 'LIVEKIT';

ALTER TABLE lms.live_sessions ADD COLUMN IF NOT EXISTS meeting_url TEXT;
ALTER TABLE lms.live_sessions ADD COLUMN IF NOT EXISTS google_event_id VARCHAR(255);
