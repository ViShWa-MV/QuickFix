package com.example.booking.repository;

import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerIdOrderByRequestedAtDesc(Long customerId);

    List<Booking> findByTechnicianProfileIdOrderByRequestedAtDesc(
            Long technicianProfileId);

    /** Pending requests that have passed their expiry — for the sweep. */
    List<Booking> findByStatusAndExpiresAtBefore(
            BookingStatus status,
            LocalDateTime cutoff);
}
