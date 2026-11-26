package com.example.booking.entity;

/**
 * Booking lifecycle. See ARCHITECTURE.md "Booking lifecycle (state machine)".
 *
 * <pre>
 * REQUESTED → CONFIRMED → EN_ROUTE → IN_PROGRESS → COMPLETED → CLOSED
 *     │  └─ DECLINED / EXPIRED            (slot released)
 *     └──────── CANCELLED (either party)  (slot released)
 * </pre>
 */
public enum BookingStatus {
    REQUESTED,
    CONFIRMED,
    EN_ROUTE,
    IN_PROGRESS,
    COMPLETED,
    CLOSED,
    DECLINED,
    EXPIRED,
    CANCELLED
}
