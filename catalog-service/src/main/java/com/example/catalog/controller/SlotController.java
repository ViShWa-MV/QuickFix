package com.example.catalog.controller;

import com.example.catalog.dto.SlotResponse;
import com.example.catalog.service.AvailabilityService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service slot state transitions, called by booking-service.
 *
 * <p>catalog-service owns slot state; booking-service never writes it directly.
 * Each transition is an atomic compare-and-set: hold (OPEN→HELD), book
 * (HELD→BOOKED), release (HELD/BOOKED→OPEN). A failed precondition returns 409.
 *
 * <p>Step 7 (hardening): restrict these to the internal caller (gateway /
 * service credentials); they are currently open.
 */
@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final AvailabilityService service;

    public SlotController(AvailabilityService service) {
        this.service = service;
    }

    @PostMapping("/{slotId}/hold")
    public SlotResponse hold(@PathVariable Long slotId) {
        return service.hold(slotId);
    }

    @PostMapping("/{slotId}/book")
    public SlotResponse book(@PathVariable Long slotId) {
        return service.book(slotId);
    }

    @PostMapping("/{slotId}/release")
    public SlotResponse release(@PathVariable Long slotId) {
        return service.release(slotId);
    }
}
