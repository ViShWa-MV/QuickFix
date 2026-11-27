package com.example.QuickFix.service;

import com.example.QuickFix.entity.PincodeOffice;
import com.example.QuickFix.repository.PincodeRepo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Checks an address against the official pincode list: the pincode must exist,
 * the state must match it, and the city must be the district or a post office
 * of that pincode. Nothing is checked on the map, and the house or flat line is
 * not checked.
 */
@Service
public class AddressVerifier {

    /** The official place names for the address, which are what gets stored. */
    public record VerifiedPlace(String state, String district) {
    }

    private final PincodeRepo pincodes;

    public AddressVerifier(PincodeRepo pincodes) {
        this.pincodes = pincodes;
    }

    /**
     * @throws IllegalArgumentException with a field-level message when a rule fails
     */
    public VerifiedPlace verify(String pincode, String state, String city) {
        List<PincodeOffice> rows = pincodes.findByPincodeOrderByOfficeAsc(pincode);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("pincode: this pincode is not in the official list");
        }

        String wantedState = PlaceNames.canonical(state);
        List<PincodeOffice> inState = rows.stream()
                .filter(r -> PlaceNames.canonical(r.getState()).equals(wantedState))
                .toList();
        if (inState.isEmpty()) {
            throw new IllegalArgumentException("state: does not match this pincode");
        }

        String wantedCity = PlaceNames.canonical(city);
        for (PincodeOffice row : inState) {
            if (PlaceNames.canonical(row.getDistrict()).equals(wantedCity)
                    || PlaceNames.canonical(row.getOffice()).equals(wantedCity)) {
                return new VerifiedPlace(row.getState(), row.getDistrict());
            }
        }
        throw new IllegalArgumentException(
                "city: does not match this pincode. Pick a city or post office from the list for this pincode.");
    }
}
