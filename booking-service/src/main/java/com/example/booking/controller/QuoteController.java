package com.example.booking.controller;

import com.example.booking.dto.QuoteRequest;
import com.example.booking.dto.QuoteResponse;
import com.example.booking.security.AuthPrincipal;
import com.example.booking.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * In-job quotes for a booking (one per booking). The technician proposes; the
 * booking's customer approves or rejects.
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
    @PreAuthorize("hasRole('TECHNICIAN')")
    public QuoteResponse propose(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long bookingId,
            @Valid @RequestBody QuoteRequest request) {
        return service.propose(me, bookingId, request);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public QuoteResponse get(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long bookingId) {
        return service.get(me, bookingId);
    }

    @PostMapping("/approve")
    @PreAuthorize("hasRole('CUSTOMER')")
    public QuoteResponse approve(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long bookingId) {
        return service.approve(me, bookingId);
    }

    @PostMapping("/reject")
    @PreAuthorize("hasRole('CUSTOMER')")
    public QuoteResponse reject(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long bookingId) {
        return service.reject(me, bookingId);
    }
}
