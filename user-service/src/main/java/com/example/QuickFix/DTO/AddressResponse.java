package com.example.QuickFix.DTO;

import com.example.QuickFix.entity.Address;
import com.example.QuickFix.entity.VerificationStatus;

/** A saved address as the owner sees it. */
public record AddressResponse(
        Long id,
        String label,
        String addressLine,
        String city,
        String state,
        String pincode,
        String streetName,
        String locality,
        Double latitude,
        Double longitude,
        VerificationStatus verificationStatus) {

    public static AddressResponse from(Address a) {
        return new AddressResponse(
                a.getId(), a.getLabel(), a.getAddressLine(), a.getCity(), a.getState(), a.getPincode(),
                a.getStreetName(), a.getLocality(), a.getLatitude(), a.getLongitude(),
                a.getVerificationStatus());
    }
}
