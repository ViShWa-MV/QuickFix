package com.example.catalog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Safety net for the booking slot-hold cycle: periodically releases holds that
 * were never confirmed or released (e.g. a booking insert failed after the hold,
 * or a release call was lost). Complements booking-service's own expiry sweep.
 */
@Component
public class SlotMaintenance {

    private static final Logger log =
            LoggerFactory.getLogger(SlotMaintenance.class);

    private final AvailabilityService availabilityService;

    public SlotMaintenance(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /** Every 5 minutes, revert stale holds back to OPEN. */
    @Scheduled(fixedDelay = 300_000L)
    public void releaseStaleHolds() {
        int freed = availabilityService.releaseStaleHolds();
        if (freed > 0) {
            log.info("Released {} stale held slot(s) back to OPEN", freed);
        }
    }
}
