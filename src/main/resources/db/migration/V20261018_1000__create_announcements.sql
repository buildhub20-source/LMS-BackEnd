-- Institute-wide announcements written by instructors and admins.
-- Publishing one also notifies active students through lms.notifications.
CREATE TABLE IF NOT EXISTS lms.announcements (
    id          UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    message     TEXT         NOT NULL,
    type        VARCHAR(20)  NOT NULL DEFAULT 'INFO'
                CHECK (type IN ('INFO', 'WARNING', 'CRITICAL', 'MAINTENANCE')),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at  TIMESTAMPTZ,
    created_by  UUID         REFERENCES lms.users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_announcements_created
    ON lms.announcements(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_announcements_active
    ON lms.announcements(active, expires_at);
