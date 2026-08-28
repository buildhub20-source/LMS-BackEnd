-- Phase 2: Course Curriculum (Modules & Lessons)
CREATE TABLE IF NOT EXISTS lms.course_modules (
    id          UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    course_id   UUID         NOT NULL REFERENCES lms.courses(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_course_modules_course ON lms.course_modules(course_id);

CREATE TABLE IF NOT EXISTS lms.lessons (
    id               UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    module_id        UUID         NOT NULL REFERENCES lms.course_modules(id) ON DELETE CASCADE,
    title            VARCHAR(255) NOT NULL,
    lesson_type      VARCHAR(30)  NOT NULL DEFAULT 'VIDEO', -- VIDEO or TEXT
    content          TEXT,                                  -- Rich text content for TEXT lessons
    recording_id     UUID         REFERENCES lms.course_recordings(id) ON DELETE SET NULL, -- for VIDEO lessons
    duration_minutes INT,
    is_free_preview  BOOLEAN      NOT NULL DEFAULT false,
    sort_order       INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_lessons_module ON lms.lessons(module_id);
