package com.example.booking.repository;

import com.example.booking.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByBookingId(Long bookingId);

    List<Review> findByBookingIdIn(Collection<Long> bookingIds);

    List<Review> findByTechnicianProfileIdOrderByCreatedAtDesc(Long technicianProfileId);

    List<Review> findByTechnicianProfileIdInOrderByCreatedAtDesc(
            Collection<Long> technicianProfileIds, Pageable pageable);
}
