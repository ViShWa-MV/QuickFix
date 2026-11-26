package com.example.booking.service;

import com.example.booking.dto.QuoteRequest;
import com.example.booking.dto.QuoteResponse;
import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.entity.Quote;
import com.example.booking.entity.QuoteLineItem;
import com.example.booking.entity.QuoteStatus;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class QuoteService {

    /** A quote can be proposed only while the job is live. */
    private static final Set<BookingStatus> QUOTABLE = Set.of(
            BookingStatus.CONFIRMED,
            BookingStatus.EN_ROUTE,
            BookingStatus.IN_PROGRESS);

    private final QuoteRepository quoteRepository;
    private final BookingRepository bookingRepository;

    public QuoteService(
            QuoteRepository quoteRepository,
            BookingRepository bookingRepository) {
        this.quoteRepository = quoteRepository;
        this.bookingRepository = bookingRepository;
    }

    /** Technician proposes a quote for extra parts/labor. */
    @Transactional
    public QuoteResponse propose(Long bookingId, QuoteRequest request) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found"));

        if (!QUOTABLE.contains(booking.getStatus())) {
            throw new IllegalStateException(
                    "A quote can only be proposed on an active booking "
                            + "(is " + booking.getStatus() + ")");
        }
        if (quoteRepository.existsByBookingId(bookingId)) {
            throw new IllegalStateException(
                    "This booking already has a quote");
        }

        List<QuoteLineItem> lineItems = request.getLineItems().stream()
                .map(li -> QuoteLineItem.builder()
                        .description(li.getDescription())
                        .amount(li.getAmount())
                        .build())
                .toList();

        double total = lineItems.stream()
                .mapToDouble(QuoteLineItem::getAmount)
                .sum();

        Quote quote = Quote.builder()
                .bookingId(bookingId)
                .lineItems(lineItems)
                .amount(total)
                .status(QuoteStatus.PROPOSED)
                .createdAt(LocalDateTime.now())
                .build();

        return QuoteResponse.from(quoteRepository.save(quote));
    }

    public QuoteResponse get(Long bookingId) {
        return QuoteResponse.from(find(bookingId));
    }

    /** Customer approves the proposed quote. */
    @Transactional
    public QuoteResponse approve(Long bookingId) {
        return decide(bookingId, QuoteStatus.APPROVED);
    }

    /** Customer rejects the proposed quote. */
    @Transactional
    public QuoteResponse reject(Long bookingId) {
        return decide(bookingId, QuoteStatus.REJECTED);
    }

    private QuoteResponse decide(Long bookingId, QuoteStatus target) {
        Quote quote = find(bookingId);
        if (quote.getStatus() != QuoteStatus.PROPOSED) {
            throw new IllegalStateException(
                    "Quote is already " + quote.getStatus());
        }
        quote.setStatus(target);
        return QuoteResponse.from(quoteRepository.save(quote));
    }

    private Quote find(Long bookingId) {
        return quoteRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No quote for this booking"));
    }
}
