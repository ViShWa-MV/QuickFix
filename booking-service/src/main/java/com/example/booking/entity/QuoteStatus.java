package com.example.booking.entity;

/**
 * Lifecycle of an in-job quote: the technician PROPOSEs extra parts/labor and
 * the customer APPROVEs or REJECTs it.
 */
public enum QuoteStatus {
    PROPOSED,
    APPROVED,
    REJECTED
}
