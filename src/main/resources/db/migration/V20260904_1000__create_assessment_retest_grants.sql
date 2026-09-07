-- ============================================================
-- assessment_retest_grants
-- ============================================================
-- When an admin/instructor grants a student a retest, a row is
-- inserted (or the extra_attempts counter is incremented) here.
-- StudentAssessmentService checks this table when the student
-- has exhausted their default attempt limit, and consumes one
-- grant to allow an additional attempt.
-- The student's previous attempts are never deleted.
-- ============================================================

CREATE TABLE IF NOT EXISTS lms.assessment_retest_grants (
    id               UUID                     NOT NULL DEFAULT gen_random_uuid(),
    assessment_id    UUID                     NOT NULL,
    student_id       UUID                     NOT NULL,
    extra_attempts   INTEGER                  NOT NULL DEFAULT 1
                         CONSTRAINT ck_retest_grants_extra_positive CHECK (extra_attempts >= 0),
    granted_by       UUID,
    granted_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_assessment_retest_grants PRIMARY KEY (id),
    CONSTRAINT uq_retest_grants_assessment_student
        UNIQUE (assessment_id, student_id),
    CONSTRAINT fk_retest_grants_assessment
        FOREIGN KEY (assessment_id) REFERENCES lms.assessments (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_retest_grants_assessment_student
    ON lms.assessment_retest_grants (assessment_id, student_id);
