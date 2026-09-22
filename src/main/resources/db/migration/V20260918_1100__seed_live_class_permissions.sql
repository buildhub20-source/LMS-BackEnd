-- ============================================================================
-- Seed Live Session permissions
-- ============================================================================

INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), seed.name, seed.resource, seed.action, seed.description
FROM (
    SELECT 'LIVE_SESSION_VIEW'   AS name, 'LIVE_SESSION' AS resource, 'VIEW'   AS action, 'View course live sessions and attendance' AS description
    UNION ALL
    SELECT 'LIVE_SESSION_MANAGE', 'LIVE_SESSION', 'MANAGE', 'Schedule, start, and manage course live sessions'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.permissions p WHERE p.name = seed.name
);

-- ADMIN gets both VIEW and MANAGE
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('LIVE_SESSION_VIEW', 'LIVE_SESSION_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- INSTRUCTOR gets both VIEW and MANAGE
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'INSTRUCTOR'
  AND p.name IN ('LIVE_SESSION_VIEW', 'LIVE_SESSION_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- STUDENT gets VIEW only
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'STUDENT'
  AND p.name IN ('LIVE_SESSION_VIEW')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
