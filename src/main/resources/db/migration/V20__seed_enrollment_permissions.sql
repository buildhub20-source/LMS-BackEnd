-- Add Enrollment permissions
INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), seed.name, seed.resource, seed.action, seed.description
FROM (
    SELECT 'ENROLLMENT_VIEW'   AS name, 'ENROLLMENT' AS resource, 'VIEW'   AS action, 'View enrollments'   AS description
    UNION ALL SELECT 'ENROLLMENT_CREATE', 'ENROLLMENT', 'CREATE', 'Create enrollments'
    UNION ALL SELECT 'ENROLLMENT_UPDATE', 'ENROLLMENT', 'UPDATE', 'Update enrollments'
    UNION ALL SELECT 'ENROLLMENT_DELETE', 'ENROLLMENT', 'DELETE', 'Delete enrollments'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.permissions p WHERE p.name = seed.name
);

-- ADMIN gets all enrollment permissions
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('ENROLLMENT_VIEW', 'ENROLLMENT_CREATE', 'ENROLLMENT_UPDATE', 'ENROLLMENT_DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- INSTRUCTOR gets VIEW, CREATE, UPDATE permissions
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'INSTRUCTOR'
  AND p.name IN ('ENROLLMENT_VIEW', 'ENROLLMENT_CREATE', 'ENROLLMENT_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
