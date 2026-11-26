package com.example.catalog.repository;

import com.example.catalog.entity.AvailabilitySlot;
import com.example.catalog.entity.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // ---- Atomic, conditional state transitions (compare-and-set) ----
    // Each returns the number of rows changed: 1 = transition applied,
    // 0 = the slot was not in the expected state (lost the race / wrong state).

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE AvailabilitySlot s
               SET s.status = com.example.catalog.entity.SlotStatus.HELD,
                   s.heldAt = :now
             WHERE s.id = :id
               AND s.status = com.example.catalog.entity.SlotStatus.OPEN
            """)
    int hold(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE AvailabilitySlot s
               SET s.status = com.example.catalog.entity.SlotStatus.BOOKED,
                   s.heldAt = null
             WHERE s.id = :id
               AND s.status = com.example.catalog.entity.SlotStatus.HELD
            """)
    int book(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE AvailabilitySlot s
               SET s.status = com.example.catalog.entity.SlotStatus.OPEN,
                   s.heldAt = null
             WHERE s.id = :id
               AND s.status IN (
                   com.example.catalog.entity.SlotStatus.HELD,
                   com.example.catalog.entity.SlotStatus.BOOKED)
            """)
    int release(@Param("id") Long id);

    /** Safety net: revert holds older than the cutoff back to OPEN. */
    @Modifying
    @Query("""
            UPDATE AvailabilitySlot s
               SET s.status = com.example.catalog.entity.SlotStatus.OPEN,
                   s.heldAt = null
             WHERE s.status = com.example.catalog.entity.SlotStatus.HELD
               AND s.heldAt < :cutoff
            """)
    int releaseStaleHolds(@Param("cutoff") LocalDateTime cutoff);
}
