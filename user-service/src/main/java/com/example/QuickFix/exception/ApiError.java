package com.example.QuickFix.exception;

import java.time.Instant;

/** The error body every QuickFix service returns: same shape in all three. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}
