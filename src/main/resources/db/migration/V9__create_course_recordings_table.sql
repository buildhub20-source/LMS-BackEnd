-- Phase 1: Course recordings — stores R2 metadata only, never the binary.
-- Each recording is currently at the course level (intro/preview).
-- Will be extended to lesson-level when lessons are built.
CREATE TABLE IF NOT EXISTS lms.course_recordings (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id        UUID         NOT NULL REFERENCES lms.courses(id) ON DELETE CASCADE,
    storage_provider VARCHAR(30)  NOT NULL DEFAULT 'CLOUDFLARE_R2',
    storage_key      VARCHAR(512) NOT NULL,
    file_name        VARCHAR(255),
    file_size        BIGINT,
    mime_type        VARCHAR(100),
    duration_seconds INT,
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    created_by       UUID         NOT NULL REFERENCES lms.users(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_course_recordings_course ON lms.course_recordings(course_id);
