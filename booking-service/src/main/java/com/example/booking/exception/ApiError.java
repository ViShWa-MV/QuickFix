package com.example.booking.exception;

import java.time.Instant;

/** Consistent JSON error body. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}
