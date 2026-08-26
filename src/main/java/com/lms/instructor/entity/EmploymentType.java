package com.lms.instructor.entity;

/**
 * How an instructor is engaged. Matches ck_instructor_profiles_employment_type.
 *
 * <p>Training centres run heavily on visiting and part-time trainers, so this is
 * a first-class field rather than something inferred from a contract elsewhere.
 */
public enum EmploymentType {
    FULL_TIME,
    PART_TIME,
    VISITING,
    CONTRACT
}
