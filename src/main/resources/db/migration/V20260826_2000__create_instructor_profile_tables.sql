-- Module: Instructor records
--
-- Mirrors student_profiles: users stays the identity/authentication record and
-- the role-specific detail lives here. Separate from student_profiles rather
-- than a shared "person profile" because user_role is many-to-many — one person
-- can be both a learner and an instructor, and would then need both rows.
--
-- No instructor_batches join table: lms.batches already carries instructor_id,
-- so an instructor's assigned batches are a query, not a second relationship.

CREATE TABLE IF NOT EXISTS lms.instructor_profiles (
    id                     UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id                UUID                     NOT NULL,
    employee_code          VARCHAR(50)              NOT NULL,
    date_of_birth          DATE,
    gender                 VARCHAR(20),
    photo_key              VARCHAR(512),
    joining_date           DATE,
    employment_type        VARCHAR(20)              NOT NULL DEFAULT 'FULL_TIME',

    -- What they teach, and how deep. Specialization is free text because a
    -- training centre adds subjects faster than a reference table can follow.
    specialization         VARCHAR(200),
    years_of_experience    NUMERIC(4, 1),
    bio                    TEXT,

    -- Education background
    highest_qualification  VARCHAR(120),
    institution            VARCHAR(200),
    year_of_completion     INTEGER,

    -- Address
    address_line1          VARCHAR(255),
    address_line2          VARCHAR(255),
    city                   VARCHAR(100),
    state                  VARCHAR(100),
    country                VARCHAR(100),
    postal_code            VARCHAR(20),

    -- Identity document
    id_proof_type          VARCHAR(30),
    id_proof_number        VARCHAR(60),

    -- One emergency contact, inline, same shape as the learner record
    emergency_contact_name     VARCHAR(150),
    emergency_contact_relation VARCHAR(60),
    emergency_contact_phone    VARCHAR(20),
    emergency_contact_email    VARCHAR(255),

    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_instructor_profiles_user FOREIGN KEY (user_id)
        REFERENCES lms.users (id) ON DELETE CASCADE,
    CONSTRAINT uk_instructor_profiles_user UNIQUE (user_id),
    CONSTRAINT uk_instructor_profiles_employee_code UNIQUE (employee_code),
    CONSTRAINT ck_instructor_profiles_gender
        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_instructor_profiles_employment_type
        CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'VISITING', 'CONTRACT')),
    CONSTRAINT ck_instructor_profiles_id_proof_type
        CHECK (id_proof_type IS NULL OR id_proof_type IN
               ('AADHAAR', 'PAN', 'PASSPORT', 'DRIVING_LICENCE', 'VOTER_ID', 'OTHER')),
    CONSTRAINT ck_instructor_profiles_experience_non_negative
        CHECK (years_of_experience IS NULL OR years_of_experience >= 0)
);

CREATE INDEX IF NOT EXISTS idx_instructor_profiles_employment
    ON lms.instructor_profiles (employment_type);
CREATE INDEX IF NOT EXISTS idx_instructor_profiles_city
    ON lms.instructor_profiles (city);

-- ============================================================
-- instructor permissions
-- ============================================================
INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), seed.name, seed.resource, seed.action, seed.description
FROM (
    SELECT 'INSTRUCTOR_VIEW'   AS name, 'INSTRUCTOR' AS resource, 'VIEW'   AS action, 'View instructor records' AS description
    UNION ALL SELECT 'INSTRUCTOR_CREATE', 'INSTRUCTOR', 'CREATE', 'Onboard a new instructor'
    UNION ALL SELECT 'INSTRUCTOR_UPDATE', 'INSTRUCTOR', 'UPDATE', 'Update instructor details'
    UNION ALL SELECT 'INSTRUCTOR_DELETE', 'INSTRUCTOR', 'DELETE', 'Remove an instructor record'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.permissions p WHERE p.name = seed.name
);

INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         CROSS JOIN lms.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('INSTRUCTOR_VIEW', 'INSTRUCTOR_CREATE', 'INSTRUCTOR_UPDATE', 'INSTRUCTOR_DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Instructors can see the instructor directory; editing stays with admin.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         JOIN lms.permissions p ON p.name = 'INSTRUCTOR_VIEW'
WHERE r.name = 'INSTRUCTOR'
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
