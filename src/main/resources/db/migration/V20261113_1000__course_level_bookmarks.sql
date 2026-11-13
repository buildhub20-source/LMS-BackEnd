-- Students can bookmark a whole course (no lesson) as well as a single lesson.
-- The existing unique constraint still stops duplicate lesson bookmarks. One
-- bookmark per course is kept by BookmarkService (it looks for the course
-- bookmark before adding one), not by a partial unique index: that is
-- PostgreSQL-only and would not run on the H2 instance the migration test uses.
ALTER TABLE student_bookmarks ALTER COLUMN lesson_id DROP NOT NULL;
