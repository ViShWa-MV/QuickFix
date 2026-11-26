package com.example.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * A customer's request to book a technician.
 *
 * <p>Step 7 (hardening): {@code customerId} should come from the authenticated
 * principal once booking-service sits behind auth, not from the body.
 */
@Getter
@Setter
public class BookingRequest {

    @NotNull
    private Long customerId;

    @NotNull
    private Long technicianProfileId;

    @NotNull
    private Long serviceId;

    @NotNull
    private Long slotId;
}
