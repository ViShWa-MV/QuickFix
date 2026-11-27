package com.example.booking.controller;

import com.example.booking.dto.AddressView;
import com.example.booking.dto.BookingRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.BookingSummaryResponse;
import com.example.booking.dto.OtpSubmitRequest;
import com.example.booking.dto.OtpView;
import com.example.booking.entity.OtpPurpose;
import com.example.booking.security.AuthPrincipal;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bookings and their lifecycle.
 *
 * <p>Role gates are applied here with {@code @PreAuthorize}; ownership (is this
 * the booking's customer or technician?) is checked in the service, where the
 * booking is loaded.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    // ---- create + read ----

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public BookingResponse create(
            @AuthenticationPrincipal AuthPrincipal me,
            @Valid @RequestBody BookingRequest request) {
        return service.create(me, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public BookingResponse getById(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long id) {
        return service.getById(me, id);
    }

    /** The address of one booking, as this caller may see it. */
    @GetMapping("/{id}/address")
    @PreAuthorize("isAuthenticated()")
    public AddressView address(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long id) {
        return service.addressFor(me, id);
    }

    /** A customer's bookings (paged). */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    public List<BookingResponse> forCustomer(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long customerId,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.getForCustomer(me, customerId, pageable);
    }

    /** A technician profile's jobs (paged). */
    @GetMapping("/technician/{technicianProfileId}")
    @PreAuthorize("isAuthenticated()")
    public List<BookingResponse> forTechnician(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long technicianProfileId,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.getForTechnician(me, technicianProfileId, pageable);
    }

    /** A customer's bookings with quote, active code, and review in one response. */
    @GetMapping("/customer/{customerId}/summary")
    @PreAuthorize("isAuthenticated()")
    public List<BookingSummaryResponse> customerSummary(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long customerId,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.customerSummary(me, customerId, pageable);
    }

    /** A technician profile's jobs with quote and review in one response. */
    @GetMapping("/technician/{technicianProfileId}/summary")
    @PreAuthorize("isAuthenticated()")
    public List<BookingSummaryResponse> technicianSummary(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long technicianProfileId,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.technicianSummary(me, technicianProfileId, pageable);
    }

    // ---- technician decisions ----

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public BookingResponse accept(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
        return service.accept(me, id);
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public BookingResponse decline(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
        return service.decline(me, id);
    }

    // ---- shared lifecycle ----

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public BookingResponse cancel(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
        return service.cancel(me, id);
    }

    @PostMapping("/{id}/en-route")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public BookingResponse enRoute(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
        return service.markEnRoute(me, id);
    }

    /** Technician reached the address: issues the customer's start code. */
    @PostMapping("/{id}/arrive")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public BookingResponse arrive(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
        return service.arrive(me, id);
    }

    /** Start work: the technician submits the customer's start code. */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public BookingResponse start(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long id,
            @Valid @RequestBody OtpSubmitRequest request) {
        return service.start(me, id, request.getOtp());
    }

    /** Work finished: issues the customer's completion code. */
    @PostMapping("/{id}/request-completion")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public BookingResponse requestCompletion(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
        return service.requestCompletion(me, id);
    }

    /** Complete: the technician submits the customer's completion code. */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public BookingResponse complete(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long id,
            @Valid @RequestBody OtpSubmitRequest request) {
        return service.complete(me, id, request.getOtp());
    }

    /**
     * The active code, for the customer of this booking only. Technicians never
     * receive it: they must be told it by the customer.
     */
    @GetMapping("/{id}/otp")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OtpView activeOtp(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long id,
            @RequestParam OtpPurpose purpose) {
        return service.getActiveOtp(me, id, purpose);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public BookingResponse close(@AuthenticationPrincipal AuthPrincipal me, @PathVariable Long id) {
        return service.close(me, id);
    }
}
