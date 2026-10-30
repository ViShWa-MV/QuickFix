package com.example.booking.controller;

import com.example.booking.dto.BookingRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bookings and their lifecycle.
 *
 * <p>Step 7 (hardening): customer/technician/admin authorization must gate these
 * (a customer cancels their own booking; the assigned technician accepts/
 * declines/advances it) once booking-service sits behind auth. Currently open.
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
    public BookingResponse create(
            @Valid @RequestBody BookingRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<BookingResponse> forCustomer(@PathVariable Long customerId) {
        return service.getForCustomer(customerId);
    }

    @GetMapping("/technician/{technicianProfileId}")
    public List<BookingResponse> forTechnician(
            @PathVariable Long technicianProfileId) {
        return service.getForTechnician(technicianProfileId);
    }

    // ---- technician decisions ----

    @PostMapping("/{id}/accept")
    public BookingResponse accept(@PathVariable Long id) {
        return service.accept(id);
    }

    @PostMapping("/{id}/decline")
    public BookingResponse decline(@PathVariable Long id) {
        return service.decline(id);
    }

    // ---- shared lifecycle ----

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @PostMapping("/{id}/en-route")
    public BookingResponse enRoute(@PathVariable Long id) {
        return service.markEnRoute(id);
    }

    @PostMapping("/{id}/start")
    public BookingResponse start(@PathVariable Long id) {
        return service.start(id);
    }

    @PostMapping("/{id}/complete")
    public BookingResponse complete(@PathVariable Long id) {
        return service.complete(id);
    }

    @PostMapping("/{id}/close")
    public BookingResponse close(@PathVariable Long id) {
        return service.close(id);
    }
}
