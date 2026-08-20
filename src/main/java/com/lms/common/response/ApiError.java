package com.lms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Standard error contract returned by {@code GlobalExceptionHandler}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private boolean success;

    private String code;

    private String message;

    private String path;

    private List<FieldViolation> errors;

    private Instant timestamp;

    public ApiError() {
    }

    public ApiError(boolean success, String code, String message, String path,
                    List<FieldViolation> errors, Instant timestamp) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.path = path;
        this.errors = errors;
        this.timestamp = timestamp;
    }

    public static ApiError of(String code, String message, String path) {
        return new ApiError(false, code, message, path, null, Instant.now());
    }

    public static ApiError of(String code, String message, String path, List<FieldViolation> errors) {
        return new ApiError(false, code, message, path, errors, Instant.now());
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<FieldViolation> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldViolation> errors) {
        this.errors = errors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /** One rejected field, as reported by bean validation. */
    public static class FieldViolation {

        private String field;

        private String message;

        public FieldViolation() {
        }

        public FieldViolation(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
