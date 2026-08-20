package com.lms.common.exception;

/** Thrown when a requested aggregate does not exist. */
public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public static ResourceNotFoundException of(String resource, Object identifier) {
        return new ResourceNotFoundException(String.format("%s not found: %s", resource, identifier));
    }
}
