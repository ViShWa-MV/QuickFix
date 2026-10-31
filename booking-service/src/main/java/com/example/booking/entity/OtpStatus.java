package com.example.booking.entity;

/** State of a single OTP. Only one ACTIVE per (booking, purpose) at a time. */
public enum OtpStatus {
    ACTIVE,
    VERIFIED,
    EXPIRED,
    VOID
}
