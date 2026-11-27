package com.example.QuickFix.DTO;

import com.example.QuickFix.entity.Address;
import com.example.QuickFix.entity.VerificationStatus;

/**
 * The address as booking-service needs it: includes the owner so the caller can
 * check it matches the customer. Only served on the internal API.
 */
public record AddressSnapshot(
        Long id,
        Long ownerUserId,
        String label,
        String addressLine,
        String locality,
        String streetName,
        String city,
        String state,
        String pincode,
        Double latitude,
        Double longitude,
        VerificationStatus verificationStatus) {

    public static AddressSnapshot from(Address a) {
        return new AddressSnapshot(
                a.getId(), a.getUserId(), a.getLabel(), a.getAddressLine(), a.getLocality(),
                a.getStreetName(), a.getCity(), a.getState(), a.getPincode(),
                a.getLatitude(), a.getLongitude(), a.getVerificationStatus());
    }
}
