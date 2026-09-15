-- ============================================================================
-- Seed default gamification data (point rules, levels, badges, milestones)
-- ============================================================================

-- Point rules
INSERT INTO lms.gamification_point_rules (id, event_type, points, active)
SELECT gen_random_uuid(), seed.event_type, seed.points, TRUE
FROM (
    SELECT 'LESSON_COMPLETION'     AS event_type, 10  AS points
    UNION ALL SELECT 'COURSE_COMPLETION',     100
    UNION ALL SELECT 'ASSESSMENT_COMPLETION', 20
    UNION ALL SELECT 'ASSESSMENT_PASS',       30
    UNION ALL SELECT 'HIGH_SCORE',            50
    UNION ALL SELECT 'DAILY_ACTIVITY',        5
    UNION ALL SELECT 'BADGE_EARNED',          15
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.gamification_point_rules pr WHERE pr.event_type = seed.event_type
);

-- Levels
INSERT INTO lms.gamification_levels (id, level_number, title, min_points, max_points, icon, color)
SELECT gen_random_uuid(), seed.level_number, seed.title, seed.min_points, seed.max_points, seed.icon, seed.color
FROM (
    SELECT 1 AS level_number, 'Beginner'  AS title, 0    AS min_points, 99   AS max_points, 'seedling'  AS icon, '#78B7D0' AS color
    UNION ALL SELECT 2, 'Learner',    100,  299,  'book-open', '#6BCB77'
    UNION ALL SELECT 3, 'Achiever',   300,  599,  'zap',       '#FFD93D'
    UNION ALL SELECT 4, 'Scholar',    600,  999,  'award',     '#FF8C32'
    UNION ALL SELECT 5, 'Expert',     1000, 1499, 'star',      '#FF6B6B'
    UNION ALL SELECT 6, 'Master',     1500, NULL, 'crown',     '#C084FC'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.gamification_levels l WHERE l.level_number = seed.level_number
);

-- Default badges
INSERT INTO lms.gamification_badges (id, name, description, icon, category, criteria_type, criteria_value, active)
SELECT gen_random_uuid(), seed.name, seed.description, seed.icon, seed.category, seed.criteria_type, seed.criteria_value, TRUE
FROM (
    SELECT 'First Lesson'        AS name, 'Complete your very first lesson'               AS description, 'book-open'       AS icon, 'Learning'    AS category, 'LESSONS_COMPLETED'  AS criteria_type, 1   AS criteria_value
    UNION ALL SELECT 'Course Completer',  'Complete an entire course',                       'graduation-cap', 'Learning',    'COURSES_COMPLETED',  1
    UNION ALL SELECT 'Assessment Ace',    'Pass your first assessment',                      'check-circle',   'Assessment',  'ASSESSMENTS_PASSED', 1
    UNION ALL SELECT 'High Scorer',       'Score 90% or above on an assessment',             'target',         'Assessment',  'HIGH_SCORE',         1
    UNION ALL SELECT 'Streak Starter',    'Maintain a 7-day learning streak',                'flame',          'Dedication',  'STREAK_DAYS',        7
    UNION ALL SELECT 'Dedicated Learner', 'Maintain a 30-day learning streak',               'fire-extinguisher','Dedication','STREAK_DAYS',        30
    UNION ALL SELECT 'Knowledge Seeker',  'Complete 5 lessons',                              'search',         'Learning',    'LESSONS_COMPLETED',  5
    UNION ALL SELECT 'Multi-Course Master','Complete 3 different courses',                    'layers',         'Learning',    'COURSES_COMPLETED',  3
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.gamification_badges b WHERE b.name = seed.name
);

-- Default milestones
INSERT INTO lms.gamification_milestones (id, milestone_key, name, description, icon, criteria_type, criteria_value, sort_order, active)
SELECT gen_random_uuid(), seed.milestone_key, seed.name, seed.description, seed.icon, seed.criteria_type, seed.criteria_value, seed.sort_order, TRUE
FROM (
    SELECT 'FIRST_LESSON'         AS milestone_key, 'First Lesson Completed'     AS name, 'You completed your first lesson!'            AS description, 'book-open'       AS icon, 'LESSONS_COMPLETED'    AS criteria_type, 1  AS criteria_value, 1 AS sort_order
    UNION ALL SELECT 'FIRST_COURSE',       'First Course Completed',     'You completed your first course!',                    'graduation-cap', 'COURSES_COMPLETED',    1,  2
    UNION ALL SELECT 'FIRST_ASSESSMENT',   'First Assessment Passed',    'You passed your first assessment!',                   'check-circle',   'ASSESSMENTS_PASSED',   1,  3
    UNION ALL SELECT 'FIVE_LESSONS',       '5 Lessons Completed',        'You have completed 5 lessons. Keep going!',           'book-open',      'LESSONS_COMPLETED',    5,  4
    UNION ALL SELECT 'TEN_LESSONS',        '10 Lessons Completed',       'You have completed 10 lessons. Impressive!',          'book-open',      'LESSONS_COMPLETED',    10, 5
    UNION ALL SELECT 'THREE_COURSES',      '3 Courses Completed',        'You have completed 3 courses. Outstanding!',          'graduation-cap', 'COURSES_COMPLETED',    3,  6
    UNION ALL SELECT 'SEVEN_DAY_STREAK',   '7-Day Streak',               'You maintained a 7-day learning streak!',             'flame',          'STREAK_DAYS',          7,  7
    UNION ALL SELECT 'THIRTY_DAY_STREAK',  '30-Day Streak',              'You maintained a 30-day learning streak. Amazing!',   'flame',          'STREAK_DAYS',          30, 8
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lms.gamification_milestones m WHERE m.milestone_key = seed.milestone_key
);
