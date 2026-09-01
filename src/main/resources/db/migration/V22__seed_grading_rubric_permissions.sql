-- Seed grading and rubric permissions for GradingController and RubricController

INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), seed.name, seed.resource, seed.action, seed.description
FROM (
    SELECT 'GRADING_VIEW'     AS name, 'GRADING' AS resource, 'VIEW'     AS action, 'View pending submissions for grading' AS description
    UNION ALL SELECT 'GRADING_EVALUATE', 'GRADING', 'EVALUATE', 'Manually evaluate and grade student submissions'
    UNION ALL SELECT 'RUBRIC_VIEW',      'RUBRIC',  'VIEW',     'View grading rubrics and criteria'
    UNION ALL SELECT 'RUBRIC_MANAGE',    'RUBRIC',  'MANAGE',   'Create and manage grading rubrics'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.permissions p WHERE p.name = seed.name
);

-- ADMIN gets all grading and rubric permissions
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('GRADING_VIEW', 'GRADING_EVALUATE', 'RUBRIC_VIEW', 'RUBRIC_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- INSTRUCTOR gets grading and rubric view/evaluate permissions
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
CROSS JOIN lms.permissions p
WHERE r.name = 'INSTRUCTOR'
  AND p.name IN ('GRADING_VIEW', 'GRADING_EVALUATE', 'RUBRIC_VIEW', 'RUBRIC_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
