package com.example.booking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expires REQUESTED bookings whose accept window has passed and releases their
 * held slots — the booking-side half of the slot-hold safety net (catalog has
 * its own stale-hold sweep too).
 */
@Component
public class BookingExpirySweep {

    private static final Logger log =
            LoggerFactory.getLogger(BookingExpirySweep.class);

    private final BookingService bookingService;

    public BookingExpirySweep(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** Every minute (requests expire after ~15 min). */
    @Scheduled(fixedDelay = 60_000L)
    public void sweep() {
        int expired = bookingService.expireStaleRequests();
        if (expired > 0) {
            log.info("Expired {} stale booking request(s)", expired);
        }
    }
}
