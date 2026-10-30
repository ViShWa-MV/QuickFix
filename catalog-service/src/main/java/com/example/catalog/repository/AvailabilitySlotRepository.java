package com.example.catalog.repository;

import com.example.catalog.entity.AvailabilitySlot;
import com.example.catalog.entity.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AvailabilitySlotRepository
        extends JpaRepository<AvailabilitySlot, Long> {

    List<AvailabilitySlot> findByTechnicianProfileIdOrderByStartAtAsc(
            Long technicianProfileId);

    boolean existsByTechnicianProfileIdAndStartAt(
            Long technicianProfileId,
            LocalDateTime startAt);

    /**
     * True if the technician has a slot in the given status that overlaps the
     * window [windowStart, windowEnd): overlap means it starts before the window
     * ends and ends after the window starts.
     */
    boolean existsByTechnicianProfileIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(
            Long technicianProfileId,
            SlotStatus status,
            LocalDateTime windowEnd,
            LocalDateTime windowStart);
}
