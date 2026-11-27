package com.example.booking.repository;

import com.example.booking.entity.BookingAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingAddressRepository extends JpaRepository<BookingAddress, Long> {

    Optional<BookingAddress> findByBookingId(Long bookingId);

    List<BookingAddress> findByBookingIdIn(Collection<Long> bookingIds);
}
