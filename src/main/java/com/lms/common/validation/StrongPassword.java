package com.lms.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * Reusable password policy constraint applied wherever a password is accepted.
 */
@Documented
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

    String message() default
            "Password must be at least 8 characters and contain an uppercase letter, a lowercase letter, a digit and a special character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
