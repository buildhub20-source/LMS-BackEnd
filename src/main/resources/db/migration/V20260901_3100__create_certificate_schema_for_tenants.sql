-- A tenant database contains both bounded contexts. The certificate service
-- connects to this dedicated database with DB_SCHEMA=cert; it never shares
-- certificate rows with another tenant.
CREATE SCHEMA IF NOT EXISTS cert;

CREATE TABLE IF NOT EXISTS cert.certificates (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    serial_number   VARCHAR(30)  NOT NULL,
    student_id      UUID         NOT NULL,
    course_id       UUID         NOT NULL,
    organization_id UUID,
    issued_at       TIMESTAMPTZ  NOT NULL,
    expires_at      TIMESTAMPTZ,
    pdf_url         VARCHAR(1024),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    revoked_at      TIMESTAMPTZ,
    revoked_by      UUID,
    revoke_reason   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_cert_serial UNIQUE (serial_number)
);

CREATE INDEX IF NOT EXISTS idx_cert_student ON cert.certificates (student_id);
CREATE INDEX IF NOT EXISTS idx_cert_course ON cert.certificates (course_id);

CREATE TABLE IF NOT EXISTS cert.certificate_templates (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    organization_id       UUID,
    background_image_url  VARCHAR(1024),
    signatory_name        VARCHAR(160),
    signatory_title       VARCHAR(160),
    signature_image_url   VARCHAR(1024),
    primary_color         VARCHAR(16),
    is_default            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

INSERT INTO cert.certificate_templates
    (id, signatory_name, signatory_title, primary_color, is_default, created_at, updated_at)
SELECT gen_random_uuid(), 'Platform Administrator', 'Chief Learning Officer', '#3B82F6', TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM cert.certificate_templates WHERE is_default = TRUE);
