-- ============================================================
-- V20260917_9000__unlock_admin_once
-- One-time replacement for the old R__unlock_admin_account
-- repeatable migration.
--
-- Unlocks the bootstrap admin account if it was locked due to
-- excessive failed login attempts (e.g. from automated testing).
-- Running this once is sufficient; the repeatable migration
-- (R__unlock_admin_account.sql) has been removed to prevent it
-- from silently unlocking a legitimately-locked account on every
-- application startup.
-- ============================================================

UPDATE lms.users
SET    is_locked = FALSE
WHERE  LOWER(email) = 'admin@lms.local'
  AND  is_locked = TRUE;
