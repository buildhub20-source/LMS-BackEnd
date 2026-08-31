-- Phase 1: Additional course lifecycle permissions not included in V6
INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), seed.name, seed.resource, seed.action, seed.description
FROM (
    SELECT 'COURSE_APPROVE'   AS name, 'COURSE' AS resource, 'APPROVE'   AS action, 'Approve a submitted course'   AS description
    UNION ALL SELECT 'COURSE_REJECT',   'COURSE', 'REJECT',   'Reject a submitted course'
    UNION ALL SELECT 'COURSE_UNPUBLISH','COURSE', 'UNPUBLISH','Unpublish a published course'
    UNION ALL SELECT 'COURSE_ARCHIVE',  'COURSE', 'ARCHIVE',  'Archive a course'
    UNION ALL SELECT 'COURSE_SUBMIT',   'COURSE', 'SUBMIT',   'Submit a course for admin review'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.permissions p WHERE p.name = seed.name
);

-- ADMIN gets APPROVE, REJECT, UNPUBLISH, ARCHIVE
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         CROSS JOIN lms.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('COURSE_APPROVE', 'COURSE_REJECT', 'COURSE_UNPUBLISH', 'COURSE_ARCHIVE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- INSTRUCTOR gets SUBMIT
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         CROSS JOIN lms.permissions p
WHERE r.name = 'INSTRUCTOR'
  AND p.name = 'COURSE_SUBMIT'
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
