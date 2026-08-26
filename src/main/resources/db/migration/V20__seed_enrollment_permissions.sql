-- Add Enrollment permissions
INSERT INTO lms.permissions (id, name, resource, action, description) VALUES
    (gen_random_uuid(), 'ENROLLMENT_VIEW',   'ENROLLMENT', 'VIEW',   'View enrollments'),
    (gen_random_uuid(), 'ENROLLMENT_CREATE', 'ENROLLMENT', 'CREATE', 'Create enrollments'),
    (gen_random_uuid(), 'ENROLLMENT_UPDATE', 'ENROLLMENT', 'UPDATE', 'Update enrollments'),
    (gen_random_uuid(), 'ENROLLMENT_DELETE', 'ENROLLMENT', 'DELETE', 'Delete enrollments')
ON CONFLICT (name) DO NOTHING;

-- ADMIN gets all enrollment permissions
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('ENROLLMENT_VIEW', 'ENROLLMENT_CREATE', 'ENROLLMENT_UPDATE', 'ENROLLMENT_DELETE')
ON CONFLICT DO NOTHING;

-- INSTRUCTOR gets VIEW, CREATE, UPDATE permissions
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'INSTRUCTOR'
  AND p.name IN ('ENROLLMENT_VIEW', 'ENROLLMENT_CREATE', 'ENROLLMENT_UPDATE')
ON CONFLICT DO NOTHING;
