package com.lms.common.exception;

/** Thrown when a JWT or invitation token cannot be trusted. */
public class InvalidTokenException extends ApplicationException {

    public InvalidTokenException(String message) {
        super(ErrorCode.TOKEN_INVALID, message);
    }
}
