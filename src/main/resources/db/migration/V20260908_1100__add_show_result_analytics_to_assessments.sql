-- ============================================================
-- V20260908_1100 — Add show_result_analytics to assessments
-- ============================================================

ALTER TABLE lms.assessments
    ADD COLUMN IF NOT EXISTS show_result_analytics BOOLEAN NOT NULL DEFAULT TRUE;
