package com.example.booking.service;

import com.example.booking.client.CatalogClient;
import com.example.booking.dto.ReviewRequest;
import com.example.booking.dto.ReviewResponse;
import com.example.booking.dto.TechnicianReviews;
import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.entity.Review;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.ReviewRepository;
import com.example.booking.security.AuthPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer feedback on a finished job. Rating and comment are each optional, but
 * a review must contain at least one of them.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final CatalogClient catalog;
    private final Access access;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            CatalogClient catalog,
            Access access) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.catalog = catalog;
        this.access = access;
    }

    /**
     * Records the customer's review and, if a rating was given, folds it into the
     * technician's average. Both happen in one transaction: if the catalog call
     * fails, the review is rolled back too.
     */
    @Transactional
    public ReviewResponse submit(AuthPrincipal me, Long bookingId, ReviewRequest request) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        access.requireCustomer(booking, me);

        if (booking.getStatus() != BookingStatus.COMPLETED
                && booking.getStatus() != BookingStatus.CLOSED) {
            throw new IllegalStateException(
                    "A job can be reviewed only after it is completed (is "
                            + booking.getStatus() + ")");
        }

        String comment = request.getComment() == null ? null : request.getComment().trim();
        if (comment != null && comment.isEmpty()) {
            comment = null;
        }
        if (request.getRating() == null && comment == null) {
            throw new IllegalArgumentException("Give a star rating, a comment, or both");
        }
        if (reviewRepository.findByBookingId(bookingId).isPresent()) {
            throw new IllegalStateException("This booking has already been reviewed");
        }

        Review review = Review.builder()
                .bookingId(bookingId)
                .technicianProfileId(booking.getTechnicianProfileId())
                .customerId(me.userId())
                .rating(request.getRating())
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();

        Review saved;
        try {
            // Flush now so a concurrent duplicate hits the unique constraint here,
            // before catalog's rating is touched.
            saved = reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("This booking has already been reviewed");
        }

        if (saved.getRating() != null) {
            catalog.addRating(booking.getTechnicianProfileId(), saved.getRating());
        }

        return ReviewResponse.from(saved);
    }

    /** The review for a booking, visible to its customer, technician, or an admin. */
    public ReviewResponse getForBooking(AuthPrincipal me, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        access.requireParticipant(booking, me);
        return reviewRepository.findByBookingId(bookingId)
                .map(ReviewResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No review for this booking"));
    }

    /**
     * The most recent reviews for each technician in a results page, in one query.
     * At most {@code perTechnician} reviews each; scans at most 200 rows in total.
     */
    public List<TechnicianReviews> recentForTechnicians(List<Long> technicianProfileIds, int perTechnician) {
        if (technicianProfileIds.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ReviewResponse>> byTechnician = new HashMap<>();
        for (Review review : reviewRepository.findByTechnicianProfileIdInOrderByCreatedAtDesc(
                technicianProfileIds, PageRequest.of(0, 200))) {
            List<ReviewResponse> list = byTechnician.computeIfAbsent(
                    review.getTechnicianProfileId(), k -> new ArrayList<>());
            if (list.size() < perTechnician) {
                list.add(ReviewResponse.from(review));
            }
        }
        return technicianProfileIds.stream()
                .map(id -> new TechnicianReviews(id, byTechnician.getOrDefault(id, List.of())))
                .toList();
    }

    public List<ReviewResponse> getForTechnician(Long technicianProfileId) {
        return reviewRepository
                .findByTechnicianProfileIdOrderByCreatedAtDesc(technicianProfileId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }
}
