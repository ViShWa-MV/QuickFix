package com.example.QuickFix.service;

import com.example.QuickFix.DTO.PincodeInfo;
import com.example.QuickFix.entity.PincodeOffice;
import com.example.QuickFix.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.example.QuickFix.repository.PincodeRepo;
import org.springframework.stereotype.Service;

import java.util.List;

/** What the official list says about a pincode, for filling the address form. */
@Service
public class PincodeLookup {

    private final PincodeRepo pincodes;

    public PincodeLookup(PincodeRepo pincodes) {
        this.pincodes = pincodes;
    }

    public PincodeInfo lookup(String pincode) {
        // An empty table means the official list was never loaded. That is a setup problem,
        // not a wrong pincode, so say so rather than calling the pincode invalid.
        if (pincodes.count() == 0) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Pincode checks are not set up yet: the official pincode list has not been loaded.");
        }
        List<PincodeOffice> rows = pincodes.findByPincodeOrderByOfficeAsc(pincode);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("This pincode is not in the official list");
        }
        PincodeOffice first = rows.get(0);
        List<String> offices = rows.stream().map(PincodeOffice::getOffice).distinct().toList();
        return new PincodeInfo(pincode, first.getState(), first.getDistrict(), offices);
    }
}
