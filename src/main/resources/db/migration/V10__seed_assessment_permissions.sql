-- Ensures assessment permissions exist and are granted to the correct roles.
--
-- V8 (seed_assessment_permissions) was created after the DB had already
-- advanced to V9, so Flyway skipped it. This V10 migration uses INSERT …
-- ON CONFLICT DO NOTHING (idempotent) so it is safe to run even if the
-- permissions already exist from V8.

INSERT INTO lms.permissions (id, name, resource, action, description)
VALUES
    (gen_random_uuid(), 'ASSESSMENT_VIEW',    'ASSESSMENT', 'VIEW',    'View assessments and attempts'),
    (gen_random_uuid(), 'ASSESSMENT_CREATE',  'ASSESSMENT', 'CREATE',  'Create assessments and questions'),
    (gen_random_uuid(), 'ASSESSMENT_UPDATE',  'ASSESSMENT', 'UPDATE',  'Update assessments and questions'),
    (gen_random_uuid(), 'ASSESSMENT_DELETE',  'ASSESSMENT', 'DELETE',  'Delete draft assessments'),
    (gen_random_uuid(), 'ASSESSMENT_PUBLISH', 'ASSESSMENT', 'PUBLISH', 'Publish and close assessments')
ON CONFLICT (name) DO NOTHING;

-- ADMIN gets all assessment permissions.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         CROSS JOIN lms.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('ASSESSMENT_VIEW', 'ASSESSMENT_CREATE', 'ASSESSMENT_UPDATE',
                 'ASSESSMENT_DELETE', 'ASSESSMENT_PUBLISH')
ON CONFLICT DO NOTHING;

-- INSTRUCTOR can view assessments.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         JOIN lms.permissions p ON p.name = 'ASSESSMENT_VIEW'
WHERE r.name = 'INSTRUCTOR'
ON CONFLICT DO NOTHING;

-- STUDENT can view published assessments assigned to them.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         JOIN lms.permissions p ON p.name = 'ASSESSMENT_VIEW'
WHERE r.name = 'STUDENT'
ON CONFLICT DO NOTHING;
