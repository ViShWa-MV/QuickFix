package com.example.booking.exception;

/**
 * Thrown when a submitted OTP is wrong but the code is still usable (attempts
 * remain). Mapped to HTTP 400. Exhausted/expired/missing codes use
 * IllegalStateException (409) instead, signalling "resend".
 */
public class OtpException extends RuntimeException {

    public OtpException(String message) {
        super(message);
    }
}
