package com.example.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * A request to book a technician for a service in one of their slots, at one of
 * the customer's verified addresses. The customer is the signed-in user, taken
 * from the token, not from this body.
 */
@Getter
@Setter
public class BookingRequest {

    @NotNull
    private Long technicianProfileId;

    @NotNull
    private Long serviceId;

    @NotNull
    private Long slotId;

    /** The customer's saved address, verified by user-service. */
    @NotNull
    private Long addressId;
}
