package com.lms.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/** Enforces the application password policy. */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern POLICY = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,128}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && POLICY.matcher(value).matches();
    }
}
