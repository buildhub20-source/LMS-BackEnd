-- Brings the certificate tables in each tenant database up to what the
-- certificate service expects.
--
-- V20260901_3100 created cert.* in tenant databases from the certificate
-- service's first migration only. That service's later migrations (V2-V5)
-- run solely against its own database, so tenant databases never received
-- them, and every certificate query against a tenant failed on a missing
-- column. This mirrors those migrations.
--
-- Every statement is safe to run where the change already exists. It sticks
-- to SQL that both PostgreSQL and H2 accept, so the CHECK constraint on
-- source_type and the partial unique index on published course templates
-- are not mirrored; the certificate service enforces both rules itself.

-- ─── certificates (service V2) ───────────────────────────────────────────────
-- Fails if a student already holds two certificates for one course; those
-- must be resolved by hand, since issuing assumes there is at most one.
CREATE UNIQUE INDEX IF NOT EXISTS uq_cert_student_course
    ON cert.certificates (student_id, course_id);
CREATE INDEX IF NOT EXISTS idx_certificates_organization_issued_at
    ON cert.certificates (organization_id, issued_at);

-- ─── certificate_templates (service V3) ──────────────────────────────────────
ALTER TABLE cert.certificate_templates
    ADD COLUMN IF NOT EXISTS name VARCHAR(160) DEFAULT 'Certificate template' NOT NULL;
ALTER TABLE cert.certificate_templates
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) DEFAULT 'LMS_RENDERED' NOT NULL;
ALTER TABLE cert.certificate_templates
    ADD COLUMN IF NOT EXISTS pdf_template_url VARCHAR(1024);
ALTER TABLE cert.certificate_templates
    ADD COLUMN IF NOT EXISTS field_layout TEXT;

-- ─── certificate_templates (service V4) ──────────────────────────────────────
ALTER TABLE cert.certificate_templates
    ADD COLUMN IF NOT EXISTS course_id UUID;
-- Added as TRUE so templates that already exist stay live, as the service's
-- V4 did; new templates then default to draft. Where the column already
-- exists nothing is added, so no draft is ever published by this.
ALTER TABLE cert.certificate_templates
    ADD COLUMN IF NOT EXISTS published BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE cert.certificate_templates
    ALTER COLUMN published SET DEFAULT FALSE;
ALTER TABLE cert.certificate_templates
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ;
UPDATE cert.certificate_templates
   SET published_at = created_at
 WHERE published = TRUE AND published_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_certificate_templates_course_published
    ON cert.certificate_templates (organization_id, course_id, published);

-- ─── certificates (service V5) ───────────────────────────────────────────────
-- Generated PDFs are no longer stored; what each certificate prints is kept
-- here instead, along with the token behind its QR verification link.
ALTER TABLE cert.certificates ADD COLUMN IF NOT EXISTS verification_token VARCHAR(64);
ALTER TABLE cert.certificates ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(255);
ALTER TABLE cert.certificates ADD COLUMN IF NOT EXISTS course_title VARCHAR(500);
ALTER TABLE cert.certificates ADD COLUMN IF NOT EXISTS issuer_name VARCHAR(255);
ALTER TABLE cert.certificates ADD COLUMN IF NOT EXISTS template_id UUID;
ALTER TABLE cert.certificates ADD COLUMN IF NOT EXISTS template_snapshot TEXT;

-- Two random UUIDs: 244 random bits, hard to guess.
UPDATE cert.certificates
   SET verification_token = REPLACE(CAST(gen_random_uuid() AS VARCHAR(36)), '-', '')
                         || REPLACE(CAST(gen_random_uuid() AS VARCHAR(36)), '-', '')
 WHERE verification_token IS NULL;
ALTER TABLE cert.certificates ALTER COLUMN verification_token SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cert_verification_token
    ON cert.certificates (verification_token);
