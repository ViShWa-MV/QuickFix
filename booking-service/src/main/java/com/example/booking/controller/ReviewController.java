package com.example.booking.controller;

import com.example.booking.dto.ReviewRequest;
import com.example.booking.dto.ReviewResponse;
import com.example.booking.dto.TechnicianReviews;
import com.example.booking.security.AuthPrincipal;
import com.example.booking.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Customer reviews of finished jobs. Reading a technician's reviews is public. */
@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    /** Submit a review for a completed booking. Only its customer may. */
    @PostMapping("/bookings/{bookingId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReviewResponse submit(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long bookingId,
            @Valid @RequestBody ReviewRequest request) {
        return service.submit(me, bookingId, request);
    }

    /** The review for this booking, for its customer or technician. */
    @GetMapping("/bookings/{bookingId}/review")
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse getForBooking(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long bookingId) {
        return service.getForBooking(me, bookingId);
    }

    /** Recent reviews for several technicians at once (public): one request per results page. */
    @GetMapping("/reviews/technicians/recent")
    public List<TechnicianReviews> recentForTechnicians(
            @RequestParam List<Long> ids,
            @RequestParam(defaultValue = "5") int perTechnician) {
        if (ids.size() > 50) {
            throw new IllegalArgumentException("At most 50 technicians per request");
        }
        return service.recentForTechnicians(ids, Math.min(Math.max(perTechnician, 1), 10));
    }

    /** All reviews for a technician, newest first (public). */
    @GetMapping("/reviews/technician/{technicianProfileId}")
    public List<ReviewResponse> forTechnician(@PathVariable Long technicianProfileId) {
        return service.getForTechnician(technicianProfileId);
    }
}
