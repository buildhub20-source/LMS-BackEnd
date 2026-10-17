package com.lms.assessment.ai;

import lombok.Getter;

/** A grading call that did not produce a suggestion. {@code retryable} means try again later. */
@Getter
public class AiGradingException extends RuntimeException {

    private final String code;
    private final boolean retryable;

    public AiGradingException(String code, String message, boolean retryable) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }
}
