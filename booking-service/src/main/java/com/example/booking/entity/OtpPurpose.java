package com.example.booking.entity;

/**
 * What an OTP gates:
 *  - START: the technician has reached the address; the customer's code must be
 *    entered to begin work (EN_ROUTE -> IN_PROGRESS).
 *  - COMPLETION: the work is done; the customer's code confirms it
 *    (IN_PROGRESS -> COMPLETED).
 */
public enum OtpPurpose {
    START,
    COMPLETION
}
