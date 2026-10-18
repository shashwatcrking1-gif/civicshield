package com.civicshield.frontend.dto;

import java.util.Map;

/** Error body returned by the backend for every 4xx/5xx response. */
public record ApiError(String timestamp, int status, String error, String message, String path,
                       Map<String, String> fieldErrors) {
    public ApiError {
        fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }
}
