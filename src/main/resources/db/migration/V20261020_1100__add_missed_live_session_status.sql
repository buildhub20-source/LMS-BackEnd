-- MISSED: a scheduled class whose time slot passed without it being started.
ALTER TABLE lms.live_sessions DROP CONSTRAINT IF EXISTS ck_live_session_status;
ALTER TABLE lms.live_sessions
    ADD CONSTRAINT ck_live_session_status
    CHECK (status IN ('SCHEDULED', 'LIVE', 'ENDED', 'CANCELLED', 'MISSED'));
