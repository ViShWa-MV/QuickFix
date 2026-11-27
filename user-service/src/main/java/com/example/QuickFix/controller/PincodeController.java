package com.example.QuickFix.controller;

import com.example.QuickFix.DTO.PincodeInfo;
import com.example.QuickFix.exception.ResourceNotFoundException;
import com.example.QuickFix.service.PincodeLookup;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Pincode lookup for the address form. Signed-in users only. */
@RestController
@RequestMapping("/api/geo/pincodes")
public class PincodeController {

    private final PincodeLookup lookup;

    public PincodeController(PincodeLookup lookup) {
        this.lookup = lookup;
    }

    @GetMapping("/{pincode}")
    public PincodeInfo get(@PathVariable String pincode) {
        if (!pincode.matches("^[0-9]{6}$")) {
            throw new IllegalArgumentException("pincode: must be six digits");
        }
        return lookup.lookup(pincode);
    }
}
