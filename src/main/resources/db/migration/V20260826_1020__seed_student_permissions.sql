-- Permissions for the student-records module, plus a repair to the ADMIN grant.
--
-- V6 seeds ADMIN by CROSS JOIN over every permission that exists at that point,
-- which makes ADMIN a superuser by construction. Later migrations have to grant
-- their own permissions explicitly to keep that invariant, and V8 missed
-- COURSE_SUBMIT (it went to INSTRUCTOR only). MigrationSchemaCheckTest asserts
-- the invariant but could never run, so the gap went unnoticed. Repaired below.
--
-- Same WHERE NOT EXISTS idempotency as V10, for the same portability reason.

INSERT INTO lms.permissions (id, name, resource, action, description)
SELECT gen_random_uuid(), seed.name, seed.resource, seed.action, seed.description
FROM (
    SELECT 'STUDENT_VIEW'   AS name, 'STUDENT' AS resource, 'VIEW'   AS action, 'View student records and guardians' AS description
    UNION ALL SELECT 'STUDENT_CREATE', 'STUDENT', 'CREATE', 'Admit a new student'
    UNION ALL SELECT 'STUDENT_UPDATE', 'STUDENT', 'UPDATE', 'Update student and guardian details'
    UNION ALL SELECT 'STUDENT_DELETE', 'STUDENT', 'DELETE', 'Remove a student record'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.permissions p WHERE p.name = seed.name
);

-- ADMIN gets every student permission.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         CROSS JOIN lms.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('STUDENT_VIEW', 'STUDENT_CREATE', 'STUDENT_UPDATE', 'STUDENT_DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- INSTRUCTOR reads student records for the classes they teach; write stays with admin.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         JOIN lms.permissions p ON p.name = 'STUDENT_VIEW'
WHERE r.name = 'INSTRUCTOR'
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Repair: restore the "ADMIN holds every permission" invariant broken by V8.
INSERT INTO lms.role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM lms.roles r
         JOIN lms.permissions p ON p.name = 'COURSE_SUBMIT'
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM lms.role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
