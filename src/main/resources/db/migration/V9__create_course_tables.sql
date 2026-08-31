-- Phase 1: Course domain table
CREATE TABLE IF NOT EXISTS lms.courses (
    id               UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    status           VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    level            VARCHAR(30),
    thumbnail_key    VARCHAR(512),
    created_by       UUID         NOT NULL REFERENCES lms.users(id),
    instructor_id    UUID         REFERENCES lms.users(id),
    duration_minutes INT,
    rejection_reason TEXT,
    created_at       TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT now(),
    published_at     TIMESTAMP WITH TIME ZONE,
    archived_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_courses_status      ON lms.courses(status);
CREATE INDEX IF NOT EXISTS idx_courses_instructor  ON lms.courses(instructor_id);
CREATE INDEX IF NOT EXISTS idx_courses_created_by  ON lms.courses(created_by);
