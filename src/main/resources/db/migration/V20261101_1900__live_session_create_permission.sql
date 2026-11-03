-- Scheduling is granted separately from general live-session management.
INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), 'LIVE_SESSION_CREATE', 'LIVE_SESSION', 'CREATE',
       'Schedule live classes using the organization calendar'
WHERE NOT EXISTS (SELECT 1 FROM lms.permissions WHERE name = 'LIVE_SESSION_CREATE');

INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id FROM lms.roles r CROSS JOIN lms.permissions p
WHERE r.name IN ('ADMIN', 'SUPER_ADMIN') AND p.name = 'LIVE_SESSION_CREATE'
  AND NOT EXISTS (SELECT 1 FROM lms.role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
