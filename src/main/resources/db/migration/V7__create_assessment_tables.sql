-- Module: Assessment
--
-- Supports the full lifecycle: Admin creates/publishes assessments with coding
-- questions and test cases; Students start attempts, write code, and submit.
-- The compiler/judge pipeline is NOT implemented here — submission status
-- columns are kept generic so the judge can write results without a schema
-- change.

-- ============================================================
-- assessments
-- ============================================================
CREATE TABLE assessments (
    id               UUID                     NOT NULL,
    title            VARCHAR(255)             NOT NULL,
    description      TEXT,
    duration_minutes INTEGER                  NOT NULL DEFAULT 60,
    total_marks      INTEGER                  NOT NULL DEFAULT 0,
    max_attempts     INTEGER                  NOT NULL DEFAULT 1,
    status           VARCHAR(30)              NOT NULL DEFAULT 'DRAFT',
    start_time       TIMESTAMP WITH TIME ZONE,
    end_time         TIMESTAMP WITH TIME ZONE,
    created_by       UUID                     NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_assessments PRIMARY KEY (id),
    CONSTRAINT fk_assessments_created_by
        FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_assessments_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'ARCHIVED')),
    CONSTRAINT ck_assessments_duration_positive
        CHECK (duration_minutes > 0),
    CONSTRAINT ck_assessments_total_marks_non_negative
        CHECK (total_marks >= 0),
    CONSTRAINT ck_assessments_max_attempts_positive
        CHECK (max_attempts > 0),
    CONSTRAINT ck_assessments_time_window
        CHECK (start_time IS NULL OR end_time IS NULL OR end_time > start_time)
);

CREATE INDEX idx_assessments_status     ON assessments (status);
CREATE INDEX idx_assessments_created_by ON assessments (created_by);
CREATE INDEX idx_assessments_start_time ON assessments (start_time);

-- ============================================================
-- questions
-- ============================================================
CREATE TABLE questions (
    id               UUID                     NOT NULL,
    title            VARCHAR(500)             NOT NULL,
    description      TEXT,
    input_format     TEXT,
    output_format    TEXT,
    constraints      TEXT,
    difficulty       VARCHAR(20)              NOT NULL DEFAULT 'MEDIUM',
    question_type    VARCHAR(30)              NOT NULL DEFAULT 'CODING',
    marks            INTEGER                  NOT NULL DEFAULT 10,
    time_limit_ms    INTEGER                  NOT NULL DEFAULT 2000,
    memory_limit_mb  INTEGER                  NOT NULL DEFAULT 256,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_questions PRIMARY KEY (id),
    CONSTRAINT ck_questions_difficulty
        CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT ck_questions_type
        CHECK (question_type IN ('CODING')),
    CONSTRAINT ck_questions_marks_positive
        CHECK (marks > 0),
    CONSTRAINT ck_questions_time_limit_positive
        CHECK (time_limit_ms > 0),
    CONSTRAINT ck_questions_memory_limit_positive
        CHECK (memory_limit_mb > 0)
);

CREATE INDEX idx_questions_difficulty ON questions (difficulty);
CREATE INDEX idx_questions_type       ON questions (question_type);

-- ============================================================
-- test_cases
-- ============================================================
CREATE TABLE test_cases (
    id              UUID    NOT NULL,
    question_id     UUID    NOT NULL,
    input_data      TEXT,
    expected_output TEXT    NOT NULL,
    is_sample       BOOLEAN NOT NULL DEFAULT FALSE,
    is_hidden       BOOLEAN NOT NULL DEFAULT TRUE,
    weight          INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT pk_test_cases PRIMARY KEY (id),
    CONSTRAINT fk_test_cases_question
        FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE,
    CONSTRAINT ck_test_cases_weight_positive
        CHECK (weight > 0)
);

CREATE INDEX idx_test_cases_question_id ON test_cases (question_id);
CREATE INDEX idx_test_cases_is_sample   ON test_cases (question_id, is_sample);

-- ============================================================
-- assessment_questions  (junction — ordered, marks overridable)
-- ============================================================
CREATE TABLE assessment_questions (
    id              UUID    NOT NULL,
    assessment_id   UUID    NOT NULL,
    question_id     UUID    NOT NULL,
    question_order  INTEGER NOT NULL DEFAULT 0,
    marks           INTEGER NOT NULL DEFAULT 10,
    CONSTRAINT pk_assessment_questions PRIMARY KEY (id),
    CONSTRAINT uk_assessment_questions_pair
        UNIQUE (assessment_id, question_id),
    CONSTRAINT fk_assessment_questions_assessment
        FOREIGN KEY (assessment_id) REFERENCES assessments (id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_questions_question
        FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE RESTRICT,
    CONSTRAINT ck_assessment_questions_marks_positive
        CHECK (marks > 0)
);

CREATE INDEX idx_assessment_questions_assessment ON assessment_questions (assessment_id);
CREATE INDEX idx_assessment_questions_order      ON assessment_questions (assessment_id, question_order);

-- ============================================================
-- assessment_attempts
-- ============================================================
CREATE TABLE assessment_attempts (
    id            UUID                     NOT NULL,
    assessment_id UUID                     NOT NULL,
    student_id    UUID                     NOT NULL,
    started_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at  TIMESTAMP WITH TIME ZONE,
    status        VARCHAR(20)              NOT NULL DEFAULT 'IN_PROGRESS',
    score         INTEGER,
    CONSTRAINT pk_assessment_attempts PRIMARY KEY (id),
    CONSTRAINT fk_assessment_attempts_assessment
        FOREIGN KEY (assessment_id) REFERENCES assessments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_assessment_attempts_student
        FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_assessment_attempts_status
        CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'SUBMITTED', 'EXPIRED')),
    CONSTRAINT ck_assessment_attempts_score_non_negative
        CHECK (score IS NULL OR score >= 0),
    CONSTRAINT ck_assessment_attempts_expires_after_start
        CHECK (expires_at > started_at)
);

CREATE INDEX idx_assessment_attempts_assessment ON assessment_attempts (assessment_id);
CREATE INDEX idx_assessment_attempts_student    ON assessment_attempts (student_id);
CREATE INDEX idx_assessment_attempts_status     ON assessment_attempts (status);
-- Enforce maxAttempts: count per (assessment_id, student_id) in application layer.
CREATE INDEX idx_assessment_attempts_pair       ON assessment_attempts (assessment_id, student_id);

-- ============================================================
-- submissions  (one row per question per attempt)
-- ============================================================
CREATE TABLE submissions (
    id            UUID                     NOT NULL,
    attempt_id    UUID                     NOT NULL,
    question_id   UUID                     NOT NULL,
    student_id    UUID                     NOT NULL,
    language      VARCHAR(50)              NOT NULL DEFAULT 'JAVA',
    source_code   TEXT,
    status        VARCHAR(30)              NOT NULL DEFAULT 'DRAFT',
    submitted_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_submissions PRIMARY KEY (id),
    CONSTRAINT uk_submissions_attempt_question
        UNIQUE (attempt_id, question_id),
    CONSTRAINT fk_submissions_attempt
        FOREIGN KEY (attempt_id) REFERENCES assessment_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_submissions_question
        FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE RESTRICT,
    CONSTRAINT fk_submissions_student
        FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE RESTRICT,
    -- Status is intentionally an open VARCHAR so the judge system can add new
    -- values without a schema migration. Application code validates known values.
    CONSTRAINT ck_submissions_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'PENDING_JUDGE', 'ACCEPTED', 'WRONG_ANSWER',
                          'COMPILATION_ERROR', 'RUNTIME_ERROR', 'TIME_LIMIT_EXCEEDED',
                          'MEMORY_LIMIT_EXCEEDED'))
);

CREATE INDEX idx_submissions_attempt_id  ON submissions (attempt_id);
CREATE INDEX idx_submissions_student_id  ON submissions (student_id);
CREATE INDEX idx_submissions_question_id ON submissions (question_id);
CREATE INDEX idx_submissions_status      ON submissions (status);
