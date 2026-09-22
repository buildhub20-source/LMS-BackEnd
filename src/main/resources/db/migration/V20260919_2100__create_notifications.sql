-- Shared notification storage used by the Spring API and notification service.
CREATE TABLE IF NOT EXISTS lms.notifications (
    id         UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
    type       VARCHAR(50)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    TEXT         NOT NULL,
    link_url   VARCHAR(500),
    data       JSONB        DEFAULT '{}',
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON lms.notifications(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read
    ON lms.notifications(user_id, is_read);
