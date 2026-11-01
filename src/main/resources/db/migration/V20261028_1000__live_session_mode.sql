ALTER TABLE lms.live_sessions ADD COLUMN IF NOT EXISTS session_mode VARCHAR(24) NOT NULL DEFAULT 'live';
UPDATE lms.live_sessions SET session_mode = 'google_meet' WHERE meeting_url IS NOT NULL;
