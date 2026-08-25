-- Seeds assessment-module permissions and grants them to the right roles.
--
-- V6 seeded the ADMIN role using CROSS JOIN on whatever permissions existed
-- at that time. New permissions added in later migrations must be explicitly
-- granted here because the CROSS JOIN already ran.

INSERT INTO permissions (id, name, resource, action, description)
VALUES
    (gen_random_uuid(), 'ASSESSMENT_VIEW',    'ASSESSMENT', 'VIEW',    'View assessments and attempts'),
    (gen_random_uuid(), 'ASSESSMENT_CREATE',  'ASSESSMENT', 'CREATE',  'Create assessments and questions'),
    (gen_random_uuid(), 'ASSESSMENT_UPDATE',  'ASSESSMENT', 'UPDATE',  'Update assessments and questions'),
    (gen_random_uuid(), 'ASSESSMENT_DELETE',  'ASSESSMENT', 'DELETE',  'Delete draft assessments'),
    (gen_random_uuid(), 'ASSESSMENT_PUBLISH', 'ASSESSMENT', 'PUBLISH', 'Publish and close assessments');

-- ADMIN gets all assessment permissions.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('ASSESSMENT_VIEW', 'ASSESSMENT_CREATE', 'ASSESSMENT_UPDATE',
                 'ASSESSMENT_DELETE', 'ASSESSMENT_PUBLISH');

-- INSTRUCTOR can view assessments (they will manage their own via instructor APIs later).
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name = 'ASSESSMENT_VIEW'
WHERE r.name = 'INSTRUCTOR';

-- STUDENT can view published assessments assigned to them.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name = 'ASSESSMENT_VIEW'
WHERE r.name = 'STUDENT';
