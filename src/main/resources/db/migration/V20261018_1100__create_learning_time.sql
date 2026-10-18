-- Time a student actually spends on lesson pages, per course per day (UTC).
-- Filled by heartbeats from the lesson player; replaces the old estimate.
CREATE TABLE IF NOT EXISTS lms.learning_time (
    id                 UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id         UUID        NOT NULL REFERENCES lms.users(id) ON DELETE CASCADE,
    course_id          UUID        NOT NULL,
    study_date         DATE        NOT NULL,
    seconds            INTEGER     NOT NULL DEFAULT 0 CHECK (seconds >= 0),
    last_heartbeat_at  TIMESTAMPTZ,
    CONSTRAINT uk_learning_time_student_course_day UNIQUE (student_id, course_id, study_date)
);

CREATE INDEX IF NOT EXISTS idx_learning_time_student_day
    ON lms.learning_time(student_id, study_date);
