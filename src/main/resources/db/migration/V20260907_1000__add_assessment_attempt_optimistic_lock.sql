-- Optimistic locking prevents concurrent grading updates from silently losing a score.
ALTER TABLE lms.assessment_attempts
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
