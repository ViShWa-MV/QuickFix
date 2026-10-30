package com.example.booking.exception;

/**
 * Thrown when a slot cannot be held or booked because it is no longer in the
 * required state (taken by someone else, expired, or released). Mapped to 409.
 */
public class SlotUnavailableException extends RuntimeException {

    public SlotUnavailableException(String message) {
        super(message);
    }
}
