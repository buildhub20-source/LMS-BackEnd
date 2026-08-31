-- Ensures assessment permissions exist and are granted to the correct roles.
--
-- Idempotent by construction: safe against databases where the rows are already
-- present and against fresh ones where they are not.
--
-- Uses WHERE NOT EXISTS rather than ON CONFLICT: H2's PostgreSQL compatibility
-- mode (what MigrationSchemaCheckTest runs on) rejects a conflict target, so
-- ON CONFLICT (name) made this migration untestable.

INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), seed.name, seed.resource, seed.action, seed.description
FROM (
    SELECT 'ASSESSMENT_VIEW'    AS name, 'ASSESSMENT' AS resource, 'VIEW'    AS action, 'View assessments and attempts'    AS description
    UNION ALL SELECT 'ASSESSMENT_CREATE',  'ASSESSMENT', 'CREATE',  'Create assessments and questions'
    UNION ALL SELECT 'ASSESSMENT_UPDATE',  'ASSESSMENT', 'UPDATE',  'Update assessments and questions'
    UNION ALL SELECT 'ASSESSMENT_DELETE',  'ASSESSMENT', 'DELETE',  'Delete draft assessments'
    UNION ALL SELECT 'ASSESSMENT_PUBLISH', 'ASSESSMENT', 'PUBLISH', 'Publish and close assessments'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.permissions p WHERE p.name = seed.name
);

-- ADMIN gets all assessment permissions.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         CROSS JOIN lms.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('ASSESSMENT_VIEW', 'ASSESSMENT_CREATE', 'ASSESSMENT_UPDATE',
                 'ASSESSMENT_DELETE', 'ASSESSMENT_PUBLISH')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- INSTRUCTOR can view assessments.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         JOIN lms.permissions p ON p.name = 'ASSESSMENT_VIEW'
WHERE r.name = 'INSTRUCTOR'
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- STUDENT can view published assessments assigned to them.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         JOIN lms.permissions p ON p.name = 'ASSESSMENT_VIEW'
WHERE r.name = 'STUDENT'
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
