package com.example.booking.exception;

/** user-service could not be reached to check the address. The booking is not made. */
public class AddressUnavailableException extends RuntimeException {

    public AddressUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
