package com.example.booking.dto;

import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private Long customerId;
    private Long technicianProfileId;
    private Long serviceId;
    private Long slotId;
    private BookingStatus status;
    private Double baseAmount;
    private LocalDateTime requestedAt;
    private LocalDateTime expiresAt;

    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getCustomerId(),
                b.getTechnicianProfileId(),
                b.getServiceId(),
                b.getSlotId(),
                b.getStatus(),
                b.getBaseAmount(),
                b.getRequestedAt(),
                b.getExpiresAt());
    }
}
