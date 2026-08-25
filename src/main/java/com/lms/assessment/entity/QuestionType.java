package com.lms.assessment.entity;

/**
 * Extensible question type.
 *
 * <p>Currently only {@code CODING} is supported. Additional types
 * (MCQ, SHORT_ANSWER, etc.) can be added without a schema migration
 * because the DB column is a VARCHAR with a CHECK constraint that
 * enumerates the known values.
 */
public enum QuestionType {
    CODING
}
