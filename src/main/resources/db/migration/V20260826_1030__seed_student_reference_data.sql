-- Starter reference data so the "Add New Student" form has populated dropdowns
-- on a fresh install. All of it is ordinary editable data, not fixed config —
-- a school is expected to rename or replace these through the admin UI.
--
-- Idempotent, so re-running never duplicates a row and never overwrites an edit.

INSERT INTO lms.academic_years (id, name, start_date, end_date, is_current)
SELECT gen_random_uuid(), seed.name, seed.start_date, seed.end_date, seed.is_current
FROM (
    SELECT '2025-2026' AS name, DATE '2025-06-01' AS start_date, DATE '2026-04-30' AS end_date, FALSE AS is_current
    UNION ALL SELECT '2026-2027', DATE '2026-06-01', DATE '2027-04-30', TRUE
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.academic_years a WHERE a.name = seed.name
);

INSERT INTO lms.school_classes (id, name, sort_order)
SELECT gen_random_uuid(), seed.name, seed.sort_order
FROM (
    SELECT 'Grade 1'  AS name,  1 AS sort_order
    UNION ALL SELECT 'Grade 2',   2
    UNION ALL SELECT 'Grade 3',   3
    UNION ALL SELECT 'Grade 4',   4
    UNION ALL SELECT 'Grade 5',   5
    UNION ALL SELECT 'Grade 6',   6
    UNION ALL SELECT 'Grade 7',   7
    UNION ALL SELECT 'Grade 8',   8
    UNION ALL SELECT 'Grade 9',   9
    UNION ALL SELECT 'Grade 10', 10
    UNION ALL SELECT 'Grade 11', 11
    UNION ALL SELECT 'Grade 12', 12
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.school_classes c WHERE c.name = seed.name
);

-- Sections A–D under every class.
INSERT INTO lms.class_sections (id, class_id, name, sort_order)
SELECT gen_random_uuid(), c.id, seed.name, seed.sort_order
FROM lms.school_classes c
         CROSS JOIN (
    SELECT 'A' AS name, 1 AS sort_order
    UNION ALL SELECT 'B', 2
    UNION ALL SELECT 'C', 3
    UNION ALL SELECT 'D', 4
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.class_sections s WHERE s.class_id = c.id AND s.name = seed.name
);

INSERT INTO lms.student_categories (id, name, description, sort_order)
SELECT gen_random_uuid(), seed.name, seed.description, seed.sort_order
FROM (
    SELECT 'General' AS name, 'General category' AS description, 1 AS sort_order
    UNION ALL SELECT 'OBC', 'Other Backward Class',  2
    UNION ALL SELECT 'SC',  'Scheduled Caste',       3
    UNION ALL SELECT 'ST',  'Scheduled Tribe',       4
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.student_categories sc WHERE sc.name = seed.name
);
