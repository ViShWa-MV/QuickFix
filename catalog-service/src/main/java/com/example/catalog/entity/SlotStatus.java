package com.example.catalog.entity;

/**
 * Lifecycle of an availability slot.
 *
 * <p>A technician publishes {@link #OPEN} slots. The {@link #HELD} and
 * {@link #BOOKED} transitions are driven by booking-service (a later step) when
 * a booking is requested and then confirmed; catalog-service only creates and
 * exposes OPEN slots.
 */
public enum SlotStatus {
    OPEN,
    HELD,
    BOOKED
}
