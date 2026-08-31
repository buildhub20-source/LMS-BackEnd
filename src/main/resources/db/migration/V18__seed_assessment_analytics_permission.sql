-- Seed ASSESSMENT_ANALYTICS_VIEW permission and grant to ADMIN, SUPER_ADMIN, and INSTRUCTOR roles.
INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), 'ASSESSMENT_ANALYTICS_VIEW', 'ASSESSMENT', 'ANALYTICS_VIEW', 'View assessment statistical analytics and score performance'
WHERE NOT EXISTS (
    SELECT 1 FROM lms.permissions p WHERE p.name = 'ASSESSMENT_ANALYTICS_VIEW'
);

-- Grant to ADMIN and SUPER_ADMIN
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name IN ('ADMIN', 'SUPER_ADMIN')
  AND p.name = 'ASSESSMENT_ANALYTICS_VIEW'
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Grant to INSTRUCTOR
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
JOIN lms.permissions p ON p.name = 'ASSESSMENT_ANALYTICS_VIEW'
WHERE r.name = 'INSTRUCTOR'
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
