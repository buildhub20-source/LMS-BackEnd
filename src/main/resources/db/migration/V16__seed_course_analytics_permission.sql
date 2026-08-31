-- Seed COURSE_ANALYTICS_VIEW permission
INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), 'COURSE_ANALYTICS_VIEW', 'COURSE', 'ANALYTICS_VIEW', 'View statistical analytics and performance metrics for courses'
WHERE NOT EXISTS (
    SELECT 1 FROM lms.permissions p WHERE p.name = 'COURSE_ANALYTICS_VIEW'
);

-- Grant COURSE_ANALYTICS_VIEW to ADMIN, SUPER_ADMIN, and INSTRUCTOR by default
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         CROSS JOIN lms.permissions p
WHERE r.name IN ('ADMIN', 'SUPER_ADMIN', 'INSTRUCTOR')
  AND p.name = 'COURSE_ANALYTICS_VIEW'
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
