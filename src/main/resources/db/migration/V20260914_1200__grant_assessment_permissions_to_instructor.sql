-- Grant assessment management permissions to the INSTRUCTOR role
-- so instructors can create, view, update, publish and delete assessments.

INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'INSTRUCTOR'
  AND p.name IN ('ASSESSMENT_VIEW', 'ASSESSMENT_CREATE', 'ASSESSMENT_UPDATE',
                 'ASSESSMENT_DELETE', 'ASSESSMENT_PUBLISH')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
