package com.example.catalog.exception;

/** The caller is signed in but may not do this (wrong role or not the owner). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
