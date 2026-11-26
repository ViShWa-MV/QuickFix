package com.example.booking.controller;

import com.example.booking.dto.QuoteRequest;
import com.example.booking.dto.QuoteResponse;
import com.example.booking.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * In-job quotes for a booking (one per booking).
 *
 * <p>Step 7 (hardening): propose must be restricted to the assigned technician,
 * approve/reject to the booking's customer. Currently open.
 */
@RestController
@RequestMapping("/api/bookings/{bookingId}/quote")
public class QuoteController {

    private final QuoteService service;

    public QuoteController(QuoteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteResponse propose(
            @PathVariable Long bookingId,
            @Valid @RequestBody QuoteRequest request) {
        return service.propose(bookingId, request);
    }

    @GetMapping
    public QuoteResponse get(@PathVariable Long bookingId) {
        return service.get(bookingId);
    }

    @PostMapping("/approve")
    public QuoteResponse approve(@PathVariable Long bookingId) {
        return service.approve(bookingId);
    }

    @PostMapping("/reject")
    public QuoteResponse reject(@PathVariable Long bookingId) {
        return service.reject(bookingId);
    }
}
