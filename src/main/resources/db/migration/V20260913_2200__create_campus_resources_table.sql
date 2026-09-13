-- ============================================================
-- V20260913_2200 — Create campus_resources table & seed permissions
-- ============================================================

CREATE TABLE IF NOT EXISTS lms.campus_resources (
    id               UUID                     NOT NULL DEFAULT gen_random_uuid(),
    title            VARCHAR(255)             NOT NULL,
    description      TEXT,
    category         VARCHAR(50)              NOT NULL,
    file_key         VARCHAR(1024)            NOT NULL,
    file_name        VARCHAR(255)             NOT NULL,
    file_type        VARCHAR(50),
    file_size        VARCHAR(50),
    file_size_bytes  BIGINT,
    target_audience  VARCHAR(50)              NOT NULL DEFAULT 'ALL_STUDENTS',
    author_id        UUID                     REFERENCES lms.users (id) ON DELETE SET NULL,
    author_name      VARCHAR(255),
    author_role      VARCHAR(50),
    downloads_count  INTEGER                  NOT NULL DEFAULT 0,
    status           VARCHAR(30)              NOT NULL DEFAULT 'PUBLISHED',
    storage_provider VARCHAR(30)              NOT NULL DEFAULT 'CLOUDFLARE_R2',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_campus_resources PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_campus_resources_category ON lms.campus_resources (category);
CREATE INDEX IF NOT EXISTS idx_campus_resources_status ON lms.campus_resources (status);
CREATE INDEX IF NOT EXISTS idx_campus_resources_author ON lms.campus_resources (author_id);
CREATE INDEX IF NOT EXISTS idx_campus_resources_created ON lms.campus_resources (created_at DESC);

-- Seed resource permissions
INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), seed.name, seed.resource, seed.action, seed.description
FROM (
    SELECT 'RESOURCE_VIEW'   AS name, 'RESOURCE' AS resource, 'VIEW'   AS action, 'View and download campus resources' AS description
    UNION ALL SELECT 'RESOURCE_CREATE', 'RESOURCE', 'CREATE', 'Upload and create campus resources'
    UNION ALL SELECT 'RESOURCE_UPDATE', 'RESOURCE', 'UPDATE', 'Update campus resource metadata'
    UNION ALL SELECT 'RESOURCE_DELETE', 'RESOURCE', 'DELETE', 'Delete campus resources'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.permissions p WHERE p.name = seed.name
);

-- Grant to ADMIN and SUPER_ADMIN
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name IN ('ADMIN', 'SUPER_ADMIN')
  AND p.name IN ('RESOURCE_VIEW', 'RESOURCE_CREATE', 'RESOURCE_UPDATE', 'RESOURCE_DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Grant to INSTRUCTOR
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'INSTRUCTOR'
  AND p.name IN ('RESOURCE_VIEW', 'RESOURCE_CREATE', 'RESOURCE_UPDATE', 'RESOURCE_DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Grant to STUDENT
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'STUDENT'
  AND p.name = 'RESOURCE_VIEW'
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Seed initial curated campus toolkit resources
INSERT INTO lms.campus_resources (id, title, description, category, file_key, file_name, file_type, file_size, file_size_bytes, target_audience, author_name, author_role, downloads_count, status, storage_provider)
VALUES 
    ('a0000001-0000-0000-0000-000000000001', 'React 19 Hooks & Architecture Patterns', 'Master compiler optimizations, Server Actions, useActionState, and clean state architecture.', 'CHEATSHEET', 'resources/cheatsheets/React_19_Hooks_Architecture.pdf', 'React_19_Hooks_Architecture.pdf', 'PDF', '1.4 MB', 1468006, 'ALL_STUDENTS', 'Platform Administrator', 'Admin', 142, 'PUBLISHED', 'CLOUDFLARE_R2'),
    ('a0000001-0000-0000-0000-000000000002', 'Java Collections & Big-O Guide', 'Deep dive into HashMap vs TreeMap, amortized complexity, and high-performance collection algorithms.', 'ACADEMIC_GUIDE', 'resources/guides/Java_Collections_Big_O.pdf', 'Java_Collections_Big_O.pdf', 'PDF', '2.1 MB', 2202009, 'ALL_STUDENTS', 'Lead Instructor', 'Instructor', 98, 'PUBLISHED', 'CLOUDFLARE_R2'),
    ('a0000001-0000-0000-0000-000000000003', 'SQL Performance & Indexing Guide', 'B-Tree indexes, composite keys, partition pruning, and EXPLAIN ANALYZE tuning techniques.', 'CHEATSHEET', 'resources/cheatsheets/SQL_Performance_Indexing.pdf', 'SQL_Performance_Indexing.pdf', 'PDF', '1.8 MB', 1887436, 'ALL_STUDENTS', 'Platform Administrator', 'Admin', 115, 'PUBLISHED', 'CLOUDFLARE_R2'),
    ('a0000001-0000-0000-0000-000000000004', 'Campus Exam & Proctoring Guidelines', 'Official academic integrity handbook, lockdown browser instructions, and proctoring requirements.', 'POLICY_EXAM', 'resources/policies/Campus_Exam_Policy_Handbook.pdf', 'Campus_Exam_Policy_Handbook.pdf', 'PDF', '850 KB', 870400, 'ALL_STUDENTS', 'Dean of Academic Operations', 'Admin', 310, 'PUBLISHED', 'CLOUDFLARE_R2'),
    ('a0000001-0000-0000-0000-000000000005', 'Git & GitHub Collaboration Workflow', 'Branching models, atomic commits, pull request review etiquette, and merge conflict resolution.', 'SOFTWARE_KIT', 'resources/software/Git_GitHub_Collaboration_Kit.zip', 'Git_GitHub_Collaboration_Kit.zip', 'ZIP', '3.2 MB', 3355443, 'ALL_STUDENTS', 'Senior Faculty Mentor', 'Instructor', 84, 'PUBLISHED', 'CLOUDFLARE_R2')
ON CONFLICT (id) DO NOTHING;
