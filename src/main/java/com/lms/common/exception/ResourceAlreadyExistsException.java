package com.lms.common.exception;

/** Thrown when creating a resource would break a uniqueness rule. */
public class ResourceAlreadyExistsException extends ApplicationException {

    public ResourceAlreadyExistsException(String message) {
        super(ErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }

    public static ResourceAlreadyExistsException of(String resource, Object identifier) {
        return new ResourceAlreadyExistsException(String.format("%s already exists: %s", resource, identifier));
    }
}
