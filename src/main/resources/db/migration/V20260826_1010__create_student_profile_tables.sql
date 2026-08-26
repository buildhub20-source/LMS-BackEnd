-- Module: Student records
--
-- Splits the school-specific detail out of `users`, which stays the identity /
-- authentication record (name, email, password, status). A person can hold both
-- the STUDENT and INSTRUCTOR roles at once — user_role is many-to-many — so the
-- role-specific detail lives in its own 1:1 table rather than in wide nullable
-- columns on `users`.
--
-- Academic year / class / section / category are reference tables rather than
-- CHECK constraints or enums: schools rename and re-open them every year, and
-- the admin UI has to be able to add one without a deployment.

-- ============================================================
-- reference data
-- ============================================================
CREATE TABLE IF NOT EXISTS lms.academic_years (
    id         UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(50)              NOT NULL,
    start_date DATE                     NOT NULL,
    end_date   DATE                     NOT NULL,
    is_current BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_academic_years_name UNIQUE (name),
    CONSTRAINT ck_academic_years_range CHECK (end_date > start_date)
);

CREATE TABLE IF NOT EXISTS lms.school_classes (
    id         UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(50)              NOT NULL,
    sort_order INTEGER                  NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_school_classes_name UNIQUE (name)
);

-- Named class_sections, not sections: `lms.sections` is already taken by the
-- assessment module (assessment_id / title / section_order) via a migration that
-- exists in the shared database but not in this repository.
CREATE TABLE IF NOT EXISTS lms.class_sections (
    id         UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    class_id   UUID                     NOT NULL,
    name       VARCHAR(50)              NOT NULL,
    sort_order INTEGER                  NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_class_sections_class FOREIGN KEY (class_id)
        REFERENCES lms.school_classes (id) ON DELETE CASCADE,
    -- Section "A" exists independently under every class.
    CONSTRAINT uk_class_sections_class_name UNIQUE (class_id, name)
);

CREATE INDEX IF NOT EXISTS idx_class_sections_class ON lms.class_sections (class_id);

CREATE TABLE IF NOT EXISTS lms.student_categories (
    id          UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(50)              NOT NULL,
    description VARCHAR(255),
    sort_order  INTEGER                  NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_student_categories_name UNIQUE (name)
);

-- ============================================================
-- student_profiles
-- ============================================================
CREATE TABLE IF NOT EXISTS lms.student_profiles (
    id               UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id          UUID                     NOT NULL,
    admission_no     VARCHAR(50)              NOT NULL,
    roll_number      VARCHAR(50),
    academic_year_id UUID                     NOT NULL,
    class_id         UUID                     NOT NULL,
    section_id       UUID                     NOT NULL,
    category_id      UUID,
    gender           VARCHAR(20),
    date_of_birth    DATE                     NOT NULL,
    photo_key        VARCHAR(512),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    -- Deleting the account deletes the record; there is no orphan student.
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id)
        REFERENCES lms.users (id) ON DELETE CASCADE,
    -- Reference rows are RESTRICT: a class with students in it must not vanish.
    CONSTRAINT fk_student_profiles_academic_year FOREIGN KEY (academic_year_id)
        REFERENCES lms.academic_years (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_profiles_class FOREIGN KEY (class_id)
        REFERENCES lms.school_classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_profiles_section FOREIGN KEY (section_id)
        REFERENCES lms.class_sections (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_profiles_category FOREIGN KEY (category_id)
        REFERENCES lms.student_categories (id) ON DELETE SET NULL,
    CONSTRAINT uk_student_profiles_user UNIQUE (user_id),
    CONSTRAINT uk_student_profiles_admission_no UNIQUE (admission_no),
    -- Roll numbers repeat across the school but not within one class+section
    -- in one year. Enforced here because it is the readable identifier staff use.
    CONSTRAINT uk_student_profiles_roll UNIQUE (academic_year_id, class_id, section_id, roll_number),
    CONSTRAINT ck_student_profiles_gender
        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_student_profiles_class_section
    ON lms.student_profiles (academic_year_id, class_id, section_id);
CREATE INDEX IF NOT EXISTS idx_student_profiles_category ON lms.student_profiles (category_id);

-- ============================================================
-- student_guardians
-- ============================================================
-- One row per relation, which is exactly what the intake form collects: a
-- father block, a mother block, and an "other guardian" block. `is_primary`
-- marks which of them is the point of contact — the form's "Select a Guardian"
-- radio. Only one row per student may carry it; that is enforced in
-- StudentServiceImpl rather than here, because a partial unique index
-- (WHERE is_primary) is PostgreSQL-only and would not run on the H2 instance
-- the migration test uses.
CREATE TABLE IF NOT EXISTS lms.student_guardians (
    id                 UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    student_profile_id UUID                     NOT NULL,
    relation           VARCHAR(20)              NOT NULL,
    name               VARCHAR(150)             NOT NULL,
    phone              VARCHAR(20),
    email              VARCHAR(255),
    occupation         VARCHAR(120),
    address            TEXT,
    photo_key          VARCHAR(512),
    is_primary         BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_student_guardians_student FOREIGN KEY (student_profile_id)
        REFERENCES lms.student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT uk_student_guardians_relation UNIQUE (student_profile_id, relation),
    CONSTRAINT ck_student_guardians_relation
        CHECK (relation IN ('FATHER', 'MOTHER', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_student_guardians_student
    ON lms.student_guardians (student_profile_id);
