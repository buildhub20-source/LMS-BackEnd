-- Add thumbnail_url to lessons table
ALTER TABLE lms.lessons ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(1024);
