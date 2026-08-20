package com.lms.common.exception;

/** Thrown when a request is well-formed but violates a business invariant. */
public class BusinessRuleException extends ApplicationException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
