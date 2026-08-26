-- Phase 1: Additional course lifecycle permissions not included in V6
INSERT INTO lms.permissions (id, name, resource, action, description) VALUES
    (gen_random_uuid(), 'COURSE_APPROVE',  'COURSE', 'APPROVE',  'Approve a submitted course'),
    (gen_random_uuid(), 'COURSE_REJECT',   'COURSE', 'REJECT',   'Reject a submitted course'),
    (gen_random_uuid(), 'COURSE_UNPUBLISH','COURSE', 'UNPUBLISH','Unpublish a published course'),
    (gen_random_uuid(), 'COURSE_ARCHIVE',  'COURSE', 'ARCHIVE',  'Archive a course'),
    (gen_random_uuid(), 'COURSE_SUBMIT',   'COURSE', 'SUBMIT',   'Submit a course for admin review')
ON CONFLICT (name) DO NOTHING;

-- ADMIN gets APPROVE, REJECT, UNPUBLISH, ARCHIVE
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         CROSS JOIN lms.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('COURSE_APPROVE', 'COURSE_REJECT', 'COURSE_UNPUBLISH', 'COURSE_ARCHIVE')
ON CONFLICT DO NOTHING;

-- INSTRUCTOR gets SUBMIT (for next phase)
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         CROSS JOIN lms.permissions p
WHERE r.name = 'INSTRUCTOR'
  AND p.name = 'COURSE_SUBMIT'
ON CONFLICT DO NOTHING;
