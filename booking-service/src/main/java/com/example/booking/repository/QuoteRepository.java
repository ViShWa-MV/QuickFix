package com.example.booking.repository;

import com.example.booking.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    Optional<Quote> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);
}
