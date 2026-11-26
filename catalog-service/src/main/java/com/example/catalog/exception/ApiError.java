package com.example.catalog.exception;

import java.time.Instant;

/**
 * Standard JSON error body returned for handled exceptions.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}
