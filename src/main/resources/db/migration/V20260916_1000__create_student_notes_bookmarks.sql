-- =====================================================
-- Student Notes & Bookmarks Tables
-- =====================================================

CREATE TABLE IF NOT EXISTS student_notes (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id      UUID NOT NULL,
    course_id       UUID NOT NULL,
    lesson_id       UUID,
    title           VARCHAR(255),
    content         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_note_student_course_lesson UNIQUE (student_id, course_id, lesson_id)
);

CREATE INDEX idx_notes_student ON student_notes (student_id);
CREATE INDEX idx_notes_student_course ON student_notes (student_id, course_id);

CREATE TABLE IF NOT EXISTS student_bookmarks (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id      UUID NOT NULL,
    course_id       UUID NOT NULL,
    lesson_id       UUID NOT NULL,
    label           VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_bookmark_student_course_lesson UNIQUE (student_id, course_id, lesson_id)
);

CREATE INDEX idx_bookmarks_student ON student_bookmarks (student_id);
CREATE INDEX idx_bookmarks_student_course ON student_bookmarks (student_id, course_id);
