package com.example.booking.dto;

import com.example.booking.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long bookingId;
    private Long technicianProfileId;
    private Long customerId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getBookingId(),
                r.getTechnicianProfileId(),
                r.getCustomerId(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt());
    }
}
