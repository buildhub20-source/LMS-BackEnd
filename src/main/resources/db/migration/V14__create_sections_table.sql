-- ============================================================
-- V14 — Create sections table and link to assessment_questions
-- ============================================================

CREATE TABLE IF NOT EXISTS lms.sections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id   UUID NOT NULL REFERENCES lms.assessments(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    section_order   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sections_assessment
    ON lms.sections(assessment_id);

-- Add nullable section_id column to assessment_questions
ALTER TABLE lms.assessment_questions
    ADD COLUMN IF NOT EXISTS section_id UUID REFERENCES lms.sections(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_aq_section
    ON lms.assessment_questions(section_id);
