-- Create enrollments table
CREATE TABLE IF NOT EXISTS lms.enrollments (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    course_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    last_accessed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES lms.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES lms.courses(id) ON DELETE CASCADE,
    CONSTRAINT uq_student_course UNIQUE (student_id, course_id)
);

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_enrollments_student_id ON lms.enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_course_id ON lms.enrollments(course_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_status ON lms.enrollments(status);
CREATE INDEX IF NOT EXISTS idx_enrollments_course_status ON lms.enrollments(course_id, status);
