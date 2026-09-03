-- Adds permissions used by the lms-certificate-service integration.
--
-- CERTIFICATE_VIEW      : list and view certificates (granted to ADMIN and STUDENT via the cert service)
-- CERTIFICATE_ISSUE     : manually issue a certificate for a student
-- CERTIFICATE_REVOKE    : revoke an active certificate
-- CERTIFICATE_TEMPLATE_MANAGE : create and update org-specific certificate templates
--
-- All four are granted to ADMIN.
-- STUDENT only needs CERTIFICATE_VIEW (their own certs are filtered in the cert service itself).

INSERT INTO permissions (id, name, resource, action, description)
VALUES
    (gen_random_uuid(), 'CERTIFICATE_VIEW',             'CERTIFICATE', 'VIEW',   'View certificates'),
    (gen_random_uuid(), 'CERTIFICATE_ISSUE',            'CERTIFICATE', 'ISSUE',  'Issue certificates to students'),
    (gen_random_uuid(), 'CERTIFICATE_REVOKE',           'CERTIFICATE', 'REVOKE', 'Revoke active certificates'),
    (gen_random_uuid(), 'CERTIFICATE_TEMPLATE_MANAGE',  'CERTIFICATE', 'MANAGE', 'Create and update certificate templates');

-- Grant all certificate permissions to ADMIN
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN (
    'CERTIFICATE_VIEW',
    'CERTIFICATE_ISSUE',
    'CERTIFICATE_REVOKE',
    'CERTIFICATE_TEMPLATE_MANAGE'
);

-- Grant CERTIFICATE_VIEW to STUDENT (their own certs only — scoped in cert service)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name = 'CERTIFICATE_VIEW'
WHERE r.name = 'STUDENT';
