-- Add compiler column to questions table
ALTER TABLE lms.questions ADD COLUMN IF NOT EXISTS compiler VARCHAR(50) DEFAULT 'ALL';
