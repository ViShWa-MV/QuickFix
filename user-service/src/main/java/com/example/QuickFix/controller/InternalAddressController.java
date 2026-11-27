package com.example.QuickFix.controller;

import com.example.QuickFix.DTO.AddressSnapshot;
import com.example.QuickFix.exception.ResourceNotFoundException;
import com.example.QuickFix.repository.AddressRepo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Called by booking-service when a booking is made. Requires the internal key. */
@RestController
@RequestMapping("/internal/addresses")
public class InternalAddressController {

    private final AddressRepo addresses;

    public InternalAddressController(AddressRepo addresses) {
        this.addresses = addresses;
    }

    @GetMapping("/{id}")
    public AddressSnapshot get(@PathVariable Long id) {
        return addresses.findById(id)
                .map(AddressSnapshot::from)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }
}
