package com.example.booking.dto;

import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;

import java.time.LocalDateTime;

/**
 * A booking with its quote, active code (customers only), and review, in one
 * response. Replaces several requests per booking on every page load.
 */
public record BookingSummaryResponse(
        Long id,
        Long customerId,
        Long technicianProfileId,
        Long serviceId,
        Long slotId,
        BookingStatus status,
        Double baseAmount,
        LocalDateTime requestedAt,
        LocalDateTime expiresAt,
        QuoteResponse quote,
        OtpView activeCode,
        ReviewResponse review,
        AddressView address) {

    public static BookingSummaryResponse of(
            Booking b, QuoteResponse quote, OtpView activeCode, ReviewResponse review, AddressView address) {
        return new BookingSummaryResponse(
                b.getId(), b.getCustomerId(), b.getTechnicianProfileId(), b.getServiceId(),
                b.getSlotId(), b.getStatus(), b.getBaseAmount(), b.getRequestedAt(),
                b.getExpiresAt(), quote, activeCode, review, address);
    }
}
