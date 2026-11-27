package com.example.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A customer's feedback on a finished job: an optional 1–5 star rating and an
 * optional comment (at least one of the two is required). At most one review per
 * booking, enforced by the unique {@code bookingId} column.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bookingId;

    @Column(nullable = false)
    private Long technicianProfileId;

    @Column(nullable = false)
    private Long customerId;

    /** 1–5, or null when the customer left only a comment. */
    private Integer rating;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
