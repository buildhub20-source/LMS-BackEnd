-- Ledger of live-class reminder mails that have already gone out.
--
-- The reminder job polls rather than holding timers in memory, so it needs a
-- durable record of what it has sent: without one a restart either re-sends
-- every reminder still inside its window or, with a narrower window, skips the
-- ones whose moment passed while the service was down. One row per session per
-- reminder kind, with the unique constraint making a double send impossible
-- even if two instances poll at once.
CREATE TABLE IF NOT EXISTS lms.live_session_reminders (
    id         UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    session_id UUID         NOT NULL REFERENCES lms.live_sessions(id) ON DELETE CASCADE,
    kind       VARCHAR(32)  NOT NULL,
    sent_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    recipients INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uq_live_session_reminder UNIQUE (session_id, kind)
);

CREATE INDEX IF NOT EXISTS idx_live_session_reminders_session
    ON lms.live_session_reminders(session_id);
