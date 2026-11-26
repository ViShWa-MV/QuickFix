package com.example.booking.exception;

/**
 * Thrown when catalog-service cannot be reached or errors unexpectedly while
 * coordinating a slot transition or price lookup. Mapped to HTTP 502.
 */
public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
