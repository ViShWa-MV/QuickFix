package com.example.booking.dto;

/**
 * An address as one viewer may see it.
 *
 * <p>{@code visibility} is FULL or AREA. AREA carries only the locality, city,
 * state, and pincode; the street, house details, and coordinates are null.
 */
public record AddressView(
        String visibility,
        String locality,
        String city,
        String state,
        String pincode,
        String streetName,
        String addressLine,
        Double latitude,
        Double longitude) {
}
