-- Creates the organisation_settings table which stores platform-wide
-- branding and contact configuration (one row expected).
--
-- NOTE: Uses IF NOT EXISTS because some environments may have had this
-- table created by an earlier manual migration that was removed from VCS.
-- created_at / updated_at are populated by Spring Data JPA auditing
-- (@CreatedDate / @LastModifiedDate in Timestamped.java).

CREATE TABLE IF NOT EXISTS organization_settings (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name            VARCHAR(160) NOT NULL,
    domain          VARCHAR(255),
    support_email   VARCHAR(255),
    description     TEXT,
    primary_color   VARCHAR(16),
    logo_url        VARCHAR(1024),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Seed a default row if none exists yet.
INSERT INTO organization_settings (id, name, primary_color, created_at, updated_at)
SELECT gen_random_uuid(), 'My LMS', '#3B82F6', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM organization_settings);
