-- Students can bookmark a whole course (no lesson) as well as a single lesson.
ALTER TABLE student_bookmarks ALTER COLUMN lesson_id DROP NOT NULL;

-- The old unique constraint does not stop duplicate course bookmarks, since
-- NULLs never compare equal; one partial index per kind does.
ALTER TABLE student_bookmarks DROP CONSTRAINT IF EXISTS uk_bookmark_student_course_lesson;
CREATE UNIQUE INDEX IF NOT EXISTS uk_bookmark_student_course_lesson
    ON student_bookmarks (student_id, course_id, lesson_id) WHERE lesson_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_bookmark_student_course_only
    ON student_bookmarks (student_id, course_id) WHERE lesson_id IS NULL;
