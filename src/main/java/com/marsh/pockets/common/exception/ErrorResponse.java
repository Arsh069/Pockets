package com.marsh.pockets.common.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    int status,
    String error,
    String message,
    Instant timestamp,
    Map<String, String> validationErrors
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, Instant.now(), null);
    }

    public ErrorResponse(int status, String error, String message, Map<String, String> validationErrors) {
        this(status, error, message, Instant.now(), validationErrors);
    }
}
