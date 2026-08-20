-- Seeds the baseline RBAC model: User -> Role -> Permission -> Action.
--
-- Permission naming is resource-first UPPER_SNAKE_CASE and is kept consistent
-- with the resource/action columns. The role and permission modules use plural
-- prefixes (ROLES_, PERMISSIONS_) so a permission can never collide with the
-- ROLE_ authority prefix Spring Security applies to role names.

INSERT INTO permissions (id, name, resource, action, description)
VALUES
    (gen_random_uuid(), 'USER_VIEW',          'USER',       'VIEW',   'View user accounts'),
    (gen_random_uuid(), 'USER_UPDATE',        'USER',       'UPDATE', 'Update user accounts'),
    (gen_random_uuid(), 'USER_DELETE',        'USER',       'DELETE', 'Deactivate user accounts'),
    (gen_random_uuid(), 'USER_MANAGE_ROLES',  'USER',       'ASSIGN', 'Assign roles to users'),
    (gen_random_uuid(), 'USER_LOCK',          'USER',       'LOCK',   'Lock and unlock user accounts'),
    (gen_random_uuid(), 'ROLES_VIEW',         'ROLE',       'VIEW',   'View roles'),
    (gen_random_uuid(), 'ROLES_MANAGE',       'ROLE',       'MANAGE', 'Create and modify roles'),
    (gen_random_uuid(), 'PERMISSIONS_VIEW',   'PERMISSION', 'VIEW',   'View permissions'),
    (gen_random_uuid(), 'PERMISSIONS_MANAGE', 'PERMISSION', 'MANAGE', 'Create and modify permissions'),
    (gen_random_uuid(), 'INVITATION_VIEW',    'INVITATION', 'VIEW',   'View invitations'),
    (gen_random_uuid(), 'INVITATION_CREATE',  'INVITATION', 'CREATE', 'Invite new users'),
    (gen_random_uuid(), 'INVITATION_MANAGE',  'INVITATION', 'MANAGE', 'Revoke and reissue invitations'),
    (gen_random_uuid(), 'SESSION_VIEW',       'SESSION',    'VIEW',   'View sessions of any user'),
    (gen_random_uuid(), 'SESSION_REVOKE',     'SESSION',    'REVOKE', 'Revoke sessions of any user'),
    (gen_random_uuid(), 'AUDIT_VIEW',         'AUDIT',      'VIEW',   'Read the audit log'),
    (gen_random_uuid(), 'COURSE_VIEW',        'COURSE',     'VIEW',   'View courses'),
    (gen_random_uuid(), 'COURSE_CREATE',      'COURSE',     'CREATE', 'Create courses'),
    (gen_random_uuid(), 'COURSE_UPDATE',      'COURSE',     'UPDATE', 'Update courses'),
    (gen_random_uuid(), 'COURSE_DELETE',      'COURSE',     'DELETE', 'Delete draft courses'),
    (gen_random_uuid(), 'COURSE_PUBLISH',     'COURSE',     'PUBLISH','Publish courses');

INSERT INTO roles (id, name, description)
VALUES
    (gen_random_uuid(), 'ADMIN',      'Manage users, roles, permissions and system configuration'),
    (gen_random_uuid(), 'INSTRUCTOR', 'Create and manage courses'),
    (gen_random_uuid(), 'STUDENT',    'Access learning features and courses');

-- ADMIN holds every permission.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

-- INSTRUCTOR owns its catalogue and can look users up.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN
                               ('COURSE_VIEW', 'COURSE_CREATE', 'COURSE_UPDATE', 'COURSE_PUBLISH', 'USER_VIEW')
WHERE r.name = 'INSTRUCTOR';

-- STUDENT can only read the catalogue.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN ('COURSE_VIEW')
WHERE r.name = 'STUDENT';
