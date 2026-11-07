-- Instructors may schedule live classes for the courses they teach.
-- LiveSessionService still rejects scheduling on a course the instructor is not assigned to.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id FROM lms.roles r CROSS JOIN lms.permissions p
WHERE r.name = 'INSTRUCTOR' AND p.name = 'LIVE_SESSION_CREATE'
  AND NOT EXISTS (SELECT 1 FROM lms.role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
