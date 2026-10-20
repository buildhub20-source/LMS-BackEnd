-- V20260913_2200 seeded five sample "campus toolkit" resources into every
-- institute, with invented download counts and authors, pointing at files that
-- were never uploaded. Remove exactly those rows (fixed seed ids); resources an
-- institute uploaded itself are untouched.
DELETE FROM lms.campus_resources
WHERE id IN (
    'a0000001-0000-0000-0000-000000000001',
    'a0000001-0000-0000-0000-000000000002',
    'a0000001-0000-0000-0000-000000000003',
    'a0000001-0000-0000-0000-000000000004',
    'a0000001-0000-0000-0000-000000000005'
);
