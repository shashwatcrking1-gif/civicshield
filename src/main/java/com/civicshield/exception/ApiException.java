package com.civicshield.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Exception for REST API errors with a specific HTTP status code, error code string,
 * and optional headers or field errors.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, String> fieldErrors;
    private final Integer retryAfterSeconds;

    public ApiException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, null, null);
    }

    public ApiException(HttpStatus status, String errorCode, String message, Map<String, String> fieldErrors, Integer retryAfterSeconds) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "bad_request", message);
    }

    public static ApiException invalidCredentials(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "forbidden", message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "conflict", message);
    }

    public static ApiException tooManyRequests(String message, int retryAfterSeconds) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too_many_requests", message, null, retryAfterSeconds);
    }
}
