-- V23: Add screen recording fields to assessment_attempts for Cloudflare R2 proctoring storage
ALTER TABLE assessment_attempts
ADD COLUMN IF NOT EXISTS recording_url VARCHAR(1024),
ADD COLUMN IF NOT EXISTS recording_key VARCHAR(512),
ADD COLUMN IF NOT EXISTS recording_duration_seconds INTEGER;
