package com.lms.common.exception;

import com.lms.common.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * Translates exceptions into the standard ApiError contract so that controllers
 * never have to deal with error shaping themselves.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiError> handleApplication(ApplicationException ex, HttpServletRequest request) {
        log.debug("Application exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return build(ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiError.of(ErrorCode.VALIDATION_FAILED.name(),
                        "Request validation failed", request.getRequestURI(), violations));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldViolation(String.valueOf(v.getPropertyPath()), v.getMessage()))
                .toList();

        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiError.of(ErrorCode.VALIDATION_FAILED.name(),
                        "Request validation failed", request.getRequestURI(), violations));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return build(ErrorCode.BAD_REQUEST, "Malformed request: " + ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(ErrorCode.UNAUTHENTICATED, "Authentication required", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.ACCESS_DENIED, "You are not allowed to perform this action", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        log.warn("Data integrity violation on {}", request.getRequestURI(), ex);
        return build(ErrorCode.RESOURCE_ALREADY_EXISTS, "The request conflicts with existing data", request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest request) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, "No endpoint " + request.getRequestURI(), request);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(org.springframework.web.multipart.MaxUploadSizeExceededException ex,
                                                                 HttpServletRequest request) {
        log.warn("Max upload size exceeded on {}", request.getRequestURI());
        return build(ErrorCode.VALIDATION_FAILED, "File size exceeds the maximum allowed upload limit (500MB).", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(ErrorCode.INTERNAL_ERROR.name(),
                        "An unexpected error occurred", request.getRequestURI()));
    }

    private ResponseEntity<ApiError> build(ErrorCode code, String message, HttpServletRequest request) {
        return ResponseEntity.status(code.status())
                .body(ApiError.of(code.name(), message, request.getRequestURI()));
    }
}
