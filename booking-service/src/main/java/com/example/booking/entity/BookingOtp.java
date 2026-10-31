package com.example.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A one-time code for a booking milestone (arrival / completion).
 *
 * <p>The customer holds the code (shown in their app today, SMS later) and the
 * technician must enter it, proving physical presence / acknowledgement.
 *
 * <p>NOTE: the code is stored retrievably because, with app-display delivery,
 * the customer reads it back from the server. When SMS delivery is added the
 * customer's copy comes from the text and this should become a one-way hash.
 */
@Entity
@Table(name = "booking_otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    @Version
    private Long version;
}
