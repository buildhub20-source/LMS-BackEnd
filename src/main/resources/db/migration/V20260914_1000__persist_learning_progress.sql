CREATE TABLE lms.enrollment_completed_lessons (
    enrollment_id UUID NOT NULL REFERENCES lms.enrollments(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lms.lessons(id) ON DELETE CASCADE,
    PRIMARY KEY (enrollment_id, lesson_id)
);
