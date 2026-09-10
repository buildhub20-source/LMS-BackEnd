-- ============================================================
-- R__unlock_admin_account (Repeatable migration)
-- Unlocks the bootstrap admin account that was auto-locked
-- due to excessive failed login attempts from automated testing.
-- ============================================================

UPDATE lms.users
SET    is_locked = FALSE
WHERE  LOWER(email) = 'admin@lms.local';
