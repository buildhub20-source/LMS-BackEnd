-- ============================================================================
-- Gamification module tables
-- ============================================================================

-- Badge definitions (admin-configurable)
CREATE TABLE IF NOT EXISTS lms.gamification_badges (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name            VARCHAR(150)  NOT NULL,
    description     TEXT,
    icon            VARCHAR(100)  NOT NULL DEFAULT 'trophy',
    category        VARCHAR(100),
    criteria_type   VARCHAR(50)   NOT NULL,
    criteria_value  INTEGER       NOT NULL DEFAULT 1,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Student badge awards (prevents duplicate awards via unique constraint)
CREATE TABLE IF NOT EXISTS lms.gamification_student_badges (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id  UUID          NOT NULL,
    badge_id    UUID          NOT NULL REFERENCES lms.gamification_badges(id) ON DELETE CASCADE,
    awarded_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_student_badge UNIQUE (student_id, badge_id)
);
CREATE INDEX IF NOT EXISTS idx_student_badges_student ON lms.gamification_student_badges(student_id);

-- Configurable point rules per event type
CREATE TABLE IF NOT EXISTS lms.gamification_point_rules (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    event_type  VARCHAR(50)   NOT NULL,
    points      INTEGER       NOT NULL DEFAULT 0,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_point_rules_event_type UNIQUE (event_type)
);

-- Immutable points ledger (append-only, idempotent via idempotency_key)
CREATE TABLE IF NOT EXISTS lms.gamification_points_ledger (
    id               UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id       UUID          NOT NULL,
    points           INTEGER       NOT NULL,
    event_type       VARCHAR(50)   NOT NULL,
    source_entity_id UUID,
    idempotency_key  VARCHAR(255)  NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_points_ledger_idempotency UNIQUE (idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_points_ledger_student ON lms.gamification_points_ledger(student_id);
CREATE INDEX IF NOT EXISTS idx_points_ledger_student_created ON lms.gamification_points_ledger(student_id, created_at);

-- Level definitions
CREATE TABLE IF NOT EXISTS lms.gamification_levels (
    id           UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    level_number INTEGER       NOT NULL,
    title        VARCHAR(100)  NOT NULL,
    min_points   INTEGER       NOT NULL,
    max_points   INTEGER,
    icon         VARCHAR(100),
    color        VARCHAR(30),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_levels_number UNIQUE (level_number)
);

-- Per-student streak tracker
CREATE TABLE IF NOT EXISTS lms.gamification_streaks (
    id                 UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id         UUID        NOT NULL,
    current_streak     INTEGER     NOT NULL DEFAULT 0,
    longest_streak     INTEGER     NOT NULL DEFAULT 0,
    last_activity_date DATE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_streaks_student UNIQUE (student_id)
);

-- Daily activity log (prevents duplicate same-day entries)
CREATE TABLE IF NOT EXISTS lms.gamification_streak_activities (
    id            UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id    UUID    NOT NULL,
    activity_date DATE    NOT NULL,
    CONSTRAINT uk_streak_activity_student_date UNIQUE (student_id, activity_date)
);
CREATE INDEX IF NOT EXISTS idx_streak_activities_student ON lms.gamification_streak_activities(student_id);

-- Milestone definitions
CREATE TABLE IF NOT EXISTS lms.gamification_milestones (
    id             UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    milestone_key  VARCHAR(100)  NOT NULL,
    name           VARCHAR(150)  NOT NULL,
    description    TEXT,
    icon           VARCHAR(100)  NOT NULL DEFAULT 'flag',
    criteria_type  VARCHAR(50)   NOT NULL,
    criteria_value INTEGER       NOT NULL DEFAULT 1,
    sort_order     INTEGER       NOT NULL DEFAULT 0,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_milestones_key UNIQUE (milestone_key)
);

-- Student milestone completions
CREATE TABLE IF NOT EXISTS lms.gamification_student_milestones (
    id           UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id   UUID          NOT NULL,
    milestone_id UUID          NOT NULL REFERENCES lms.gamification_milestones(id) ON DELETE CASCADE,
    completed_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_student_milestone UNIQUE (student_id, milestone_id)
);
CREATE INDEX IF NOT EXISTS idx_student_milestones_student ON lms.gamification_student_milestones(student_id);
