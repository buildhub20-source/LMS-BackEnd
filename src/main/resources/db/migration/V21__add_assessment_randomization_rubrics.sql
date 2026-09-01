-- Migration V21: Add assessment randomization, retake policies, and rubrics tables

-- 1. Add randomize_questions and retake_policy to assessments table
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS randomize_questions BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS retake_policy VARCHAR(30) NOT NULL DEFAULT 'BEST_SCORE';

-- 2. Create rubrics table
CREATE TABLE IF NOT EXISTS rubrics (
    id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_rubrics PRIMARY KEY (id)
);

-- 3. Create rubric_criteria table
CREATE TABLE IF NOT EXISTS rubric_criteria (
    id UUID NOT NULL,
    rubric_id UUID NOT NULL,
    criterion_name VARCHAR(255) NOT NULL,
    description TEXT,
    max_points INT NOT NULL DEFAULT 10,
    weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    CONSTRAINT pk_rubric_criteria PRIMARY KEY (id),
    CONSTRAINT fk_rubric_criteria_rubric FOREIGN KEY (rubric_id) REFERENCES rubrics(id) ON DELETE CASCADE
);

-- 4. Create rubric_scores table for evaluation submissions
CREATE TABLE IF NOT EXISTS rubric_scores (
    id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    submission_id UUID NOT NULL,
    criterion_id UUID NOT NULL,
    score INT NOT NULL,
    feedback TEXT,
    evaluator_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_rubric_scores PRIMARY KEY (id),
    CONSTRAINT fk_rubric_scores_attempt FOREIGN KEY (attempt_id) REFERENCES assessment_attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_rubric_scores_submission FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_rubric_scores_criterion FOREIGN KEY (criterion_id) REFERENCES rubric_criteria(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_rubric_scores_attempt ON rubric_scores(attempt_id);
CREATE INDEX IF NOT EXISTS idx_rubric_scores_submission ON rubric_scores(submission_id);
