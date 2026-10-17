-- Each graded answer keeps its own score; the attempt total is the sum of its
-- answers instead of being overwritten by whichever answer was graded last.
ALTER TABLE lms.submissions ADD COLUMN IF NOT EXISTS score INTEGER;

-- AI grading suggestions. An instructor reviews each one before any score is
-- saved; the row records what the AI proposed and what it cost.
CREATE TABLE IF NOT EXISTS lms.ai_grading_results (
    id             UUID         NOT NULL,
    submission_id  UUID         NOT NULL,
    requested_by   UUID         NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    score          NUMERIC(8, 2),
    max_score      INTEGER,
    tests_passed   INTEGER,
    tests_total    INTEGER,
    result_json    TEXT,
    error_code     VARCHAR(50),
    error_message  VARCHAR(500),
    input_tokens   INTEGER,
    output_tokens  INTEGER,
    created_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_ai_grading_results PRIMARY KEY (id),
    CONSTRAINT fk_ai_grading_results_submission
        FOREIGN KEY (submission_id) REFERENCES lms.submissions (id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_grading_results_status
        CHECK (status IN ('SUGGESTED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_ai_grading_results_submission
    ON lms.ai_grading_results (submission_id, created_at);
