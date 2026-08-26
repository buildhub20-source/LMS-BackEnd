-- Reshapes student records from a K-12 schools model to a training-centre /
-- college model.
--
-- The previous migration modelled grades, sections and parent/guardian blocks.
-- That is a schools domain: adult learners have no guardians, and the grouping
-- unit here is a batch (a dated cohort running one course), not class + section.
--
-- Fix-forward rather than editing V20260826_1010, per docs/database/migrations.md
-- rule 1 — that migration is already applied on the shared database.
--
-- Safe to drop: student_profiles and student_guardians held no rows, and the
-- school_classes / class_sections / academic_years rows were seed data from
-- V20260826_1030 only.
--
-- Scope decisions taken with the product owner:
--   * single organisation, so there is no training_centres table
--   * no semesters or terms; batch start/end dates carry the calendar, which is
--     also why academic_years goes
--   * a learner may sit in several batches at once, so enrolment is a join table

DROP TABLE IF EXISTS lms.student_guardians;
DROP TABLE IF EXISTS lms.student_profiles;
DROP TABLE IF EXISTS lms.class_sections;
DROP TABLE IF EXISTS lms.school_classes;
DROP TABLE IF EXISTS lms.academic_years;

-- ============================================================
-- batches — a dated cohort running one course
-- ============================================================
CREATE TABLE IF NOT EXISTS lms.batches (
    id             UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    code           VARCHAR(50)              NOT NULL,
    name           VARCHAR(150)             NOT NULL,
    -- Hangs off the existing course catalogue rather than a parallel programme
    -- hierarchy. Nullable so a batch can be scheduled before its course exists.
    course_id      UUID,
    instructor_id  UUID,
    start_date     DATE                     NOT NULL,
    end_date       DATE,
    schedule       VARCHAR(150),
    delivery_mode  VARCHAR(20)              NOT NULL DEFAULT 'OFFLINE',
    capacity       INTEGER,
    status         VARCHAR(20)              NOT NULL DEFAULT 'PLANNED',
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_batches_code UNIQUE (code),
    CONSTRAINT fk_batches_course FOREIGN KEY (course_id)
        REFERENCES lms.courses (id) ON DELETE SET NULL,
    CONSTRAINT fk_batches_instructor FOREIGN KEY (instructor_id)
        REFERENCES lms.users (id) ON DELETE SET NULL,
    CONSTRAINT ck_batches_status
        CHECK (status IN ('PLANNED', 'ONGOING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_batches_delivery_mode
        CHECK (delivery_mode IN ('OFFLINE', 'ONLINE', 'HYBRID')),
    CONSTRAINT ck_batches_capacity_positive
        CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT ck_batches_dates
        CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_batches_status     ON lms.batches (status);
CREATE INDEX IF NOT EXISTS idx_batches_course     ON lms.batches (course_id);
CREATE INDEX IF NOT EXISTS idx_batches_instructor ON lms.batches (instructor_id);
CREATE INDEX IF NOT EXISTS idx_batches_start_date ON lms.batches (start_date);

-- ============================================================
-- student_profiles — the learner record
-- ============================================================
CREATE TABLE IF NOT EXISTS lms.student_profiles (
    id                     UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id                UUID                     NOT NULL,
    registration_no        VARCHAR(50)              NOT NULL,
    date_of_birth          DATE,
    gender                 VARCHAR(20),
    photo_key              VARCHAR(512),
    category_id            UUID,
    admission_date         DATE,

    -- Education background
    highest_qualification  VARCHAR(120),
    institution            VARCHAR(200),
    year_of_completion     INTEGER,

    -- Working learners are the norm at a training centre, not the exception.
    employer               VARCHAR(200),
    work_experience_years  NUMERIC(4, 1),

    -- Address
    address_line1          VARCHAR(255),
    address_line2          VARCHAR(255),
    city                   VARCHAR(100),
    state                  VARCHAR(100),
    country                VARCHAR(100),
    postal_code            VARCHAR(20),

    -- Identity document, for certification and attendance verification
    id_proof_type          VARCHAR(30),
    id_proof_number        VARCHAR(60),

    -- One emergency contact, inline: adults have a contact, not a guardian set.
    emergency_contact_name     VARCHAR(150),
    emergency_contact_relation VARCHAR(60),
    emergency_contact_phone    VARCHAR(20),
    emergency_contact_email    VARCHAR(255),

    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id)
        REFERENCES lms.users (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_profiles_category FOREIGN KEY (category_id)
        REFERENCES lms.student_categories (id) ON DELETE SET NULL,
    CONSTRAINT uk_student_profiles_user UNIQUE (user_id),
    CONSTRAINT uk_student_profiles_registration_no UNIQUE (registration_no),
    CONSTRAINT ck_student_profiles_gender
        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_student_profiles_id_proof_type
        CHECK (id_proof_type IS NULL OR id_proof_type IN
               ('AADHAAR', 'PAN', 'PASSPORT', 'DRIVING_LICENCE', 'VOTER_ID', 'OTHER')),
    CONSTRAINT ck_student_profiles_experience_non_negative
        CHECK (work_experience_years IS NULL OR work_experience_years >= 0)
);

CREATE INDEX IF NOT EXISTS idx_student_profiles_category ON lms.student_profiles (category_id);
CREATE INDEX IF NOT EXISTS idx_student_profiles_city     ON lms.student_profiles (city);

-- ============================================================
-- student_batches — enrolment, many-to-many over time
-- ============================================================
-- A learner can sit in several batches at once (a Java track and a soft-skills
-- module, say), so this is a join table rather than a column on the profile.
CREATE TABLE IF NOT EXISTS lms.student_batches (
    id                 UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    student_profile_id UUID                     NOT NULL,
    batch_id           UUID                     NOT NULL,
    enrolled_on        DATE                     NOT NULL DEFAULT CURRENT_DATE,
    status             VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    completed_on       DATE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_student_batches_student FOREIGN KEY (student_profile_id)
        REFERENCES lms.student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_batches_batch FOREIGN KEY (batch_id)
        REFERENCES lms.batches (id) ON DELETE RESTRICT,
    -- Re-joining the same batch later would be a status change, not a new row.
    CONSTRAINT uk_student_batches_pair UNIQUE (student_profile_id, batch_id),
    CONSTRAINT ck_student_batches_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'DROPPED', 'ON_HOLD'))
);

CREATE INDEX IF NOT EXISTS idx_student_batches_student ON lms.student_batches (student_profile_id);
CREATE INDEX IF NOT EXISTS idx_student_batches_batch   ON lms.student_batches (batch_id);
CREATE INDEX IF NOT EXISTS idx_student_batches_status  ON lms.student_batches (status);

-- ============================================================
-- batch permissions
-- ============================================================
INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), seed.name, seed.resource, seed.action, seed.description
FROM (
    SELECT 'BATCH_VIEW'   AS name, 'BATCH' AS resource, 'VIEW'   AS action, 'View batches and their learners' AS description
    UNION ALL SELECT 'BATCH_CREATE', 'BATCH', 'CREATE', 'Create a batch'
    UNION ALL SELECT 'BATCH_UPDATE', 'BATCH', 'UPDATE', 'Update a batch'
    UNION ALL SELECT 'BATCH_DELETE', 'BATCH', 'DELETE', 'Delete an empty batch'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.permissions p WHERE p.name = seed.name
);

INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         CROSS JOIN lms.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('BATCH_VIEW', 'BATCH_CREATE', 'BATCH_UPDATE', 'BATCH_DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Instructors read the batches they teach; scheduling stays with admin.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         JOIN lms.permissions p ON p.name = 'BATCH_VIEW'
WHERE r.name = 'INSTRUCTOR'
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
