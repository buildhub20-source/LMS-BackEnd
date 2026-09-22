-- ============================================================================
-- LMS Live Classes module tables & tenant configurations
-- ============================================================================

-- 1. Extend platform tenant configurations with Live Class limits and toggles
-- Keep each ADD COLUMN in its own statement. PostgreSQL accepts a comma-separated
-- form, but H2 (used by the migration verification tests) does not combine that
-- form with IF NOT EXISTS.
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS live_classes_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS max_live_participants INT NOT NULL DEFAULT 50;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS monthly_live_participant_minutes INT NOT NULL DEFAULT 3000;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS max_live_session_duration_minutes INT NOT NULL DEFAULT 120;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS max_concurrent_live_sessions INT NOT NULL DEFAULT 2;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS recording_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS monthly_recording_minutes INT NOT NULL DEFAULT 600;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS attendance_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS live_chat_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS screen_share_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS ai_transcript_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE platform.tenant_configs ADD COLUMN IF NOT EXISTS ai_summary_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Create Live Sessions table in lms schema
CREATE TABLE IF NOT EXISTS lms.live_sessions (
    id               UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id        UUID,
    course_id        UUID         NOT NULL REFERENCES lms.courses(id) ON DELETE CASCADE,
    instructor_id    UUID         NOT NULL REFERENCES lms.users(id),
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    scheduled_start  TIMESTAMPTZ  NOT NULL,
    scheduled_end    TIMESTAMPTZ  NOT NULL,
    actual_start     TIMESTAMPTZ,
    actual_end       TIMESTAMPTZ,
    room_name        VARCHAR(255) NOT NULL,
    status           VARCHAR(32)  NOT NULL DEFAULT 'SCHEDULED',
    recording_url    TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_live_session_status CHECK (status IN ('SCHEDULED', 'LIVE', 'ENDED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_live_sessions_course ON lms.live_sessions(course_id);
CREATE INDEX IF NOT EXISTS idx_live_sessions_instructor ON lms.live_sessions(instructor_id);
CREATE INDEX IF NOT EXISTS idx_live_sessions_status ON lms.live_sessions(status);
CREATE INDEX IF NOT EXISTS idx_live_sessions_schedule ON lms.live_sessions(scheduled_start, scheduled_end);

-- 3. Create Live Session Attendance table in lms schema
CREATE TABLE IF NOT EXISTS lms.live_session_attendance (
    id               UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    session_id       UUID         NOT NULL REFERENCES lms.live_sessions(id) ON DELETE CASCADE,
    user_id          UUID         NOT NULL REFERENCES lms.users(id),
    joined_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    left_at          TIMESTAMPTZ,
    duration_seconds BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_live_attendance_session ON lms.live_session_attendance(session_id);
CREATE INDEX IF NOT EXISTS idx_live_attendance_user ON lms.live_session_attendance(user_id);
CREATE INDEX IF NOT EXISTS idx_live_attendance_session_user ON lms.live_session_attendance(session_id, user_id);

-- 4. Create Tenant Monthly Live Usage table in lms schema
CREATE TABLE IF NOT EXISTS lms.tenant_live_usage (
    id                          UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id                   UUID         NOT NULL,
    billing_month               VARCHAR(7)   NOT NULL, -- Format: YYYY-MM
    participant_minutes_used    BIGINT       NOT NULL DEFAULT 0,
    recording_minutes_used      BIGINT       NOT NULL DEFAULT 0,
    sessions_hosted             INT          NOT NULL DEFAULT 0,
    peak_concurrent_participants INT          NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_tenant_live_usage_month UNIQUE (tenant_id, billing_month)
);

CREATE INDEX IF NOT EXISTS idx_tenant_live_usage_tenant ON lms.tenant_live_usage(tenant_id);
