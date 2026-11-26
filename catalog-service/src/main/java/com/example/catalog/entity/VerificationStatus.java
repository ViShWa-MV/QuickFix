package com.example.catalog.entity;

/**
 * Lifecycle of a technician profile's verification by an admin.
 * A newly registered profile starts {@link #PENDING} and only becomes
 * discoverable once an admin moves it to {@link #APPROVED}.
 */
public enum VerificationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
