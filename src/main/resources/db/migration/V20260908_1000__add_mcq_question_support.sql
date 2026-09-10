-- ============================================================
-- V20260908_1000 — Add MCQ question support & question_options
-- ============================================================

-- 1. Update question_type check constraint to support MULTIPLE_CHOICE
ALTER TABLE lms.questions DROP CONSTRAINT IF EXISTS ck_questions_type;

ALTER TABLE lms.questions
    ADD CONSTRAINT ck_questions_type
    CHECK (question_type IN ('CODING', 'MULTIPLE_CHOICE'));

-- 2. Create question_options table
CREATE TABLE IF NOT EXISTS lms.question_options (
    id              UUID                     NOT NULL DEFAULT gen_random_uuid(),
    question_id     UUID                     NOT NULL,
    option_text     TEXT                     NOT NULL,
    is_correct      BOOLEAN                  NOT NULL DEFAULT FALSE,
    order_index     INTEGER                  NOT NULL DEFAULT 0,
    explanation     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_question_options PRIMARY KEY (id),
    CONSTRAINT fk_question_options_question
        FOREIGN KEY (question_id) REFERENCES lms.questions (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_question_options_question
    ON lms.question_options (question_id);
