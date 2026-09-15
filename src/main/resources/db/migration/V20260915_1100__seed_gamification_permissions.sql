-- ============================================================================
-- Seed gamification permissions
-- ============================================================================

-- Insert GAMIFICATION_VIEW and GAMIFICATION_MANAGE permissions
INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), seed.name, seed.resource, seed.action, seed.description
FROM (
    SELECT 'GAMIFICATION_VIEW'   AS name, 'GAMIFICATION' AS resource, 'VIEW'   AS action, 'View gamification data (dashboard, badges, leaderboard)' AS description
    UNION ALL
    SELECT 'GAMIFICATION_MANAGE', 'GAMIFICATION', 'MANAGE', 'Manage gamification settings (badges, levels, milestones, point rules)'
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
  AND p.name IN ('GAMIFICATION_VIEW', 'GAMIFICATION_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- INSTRUCTOR gets VIEW only
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'INSTRUCTOR'
  AND p.name IN ('GAMIFICATION_VIEW')
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
  AND p.name IN ('GAMIFICATION_VIEW')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
