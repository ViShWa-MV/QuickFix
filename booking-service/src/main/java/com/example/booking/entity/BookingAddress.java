package com.example.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** The address of one booking, copied from the customer's verified address. */
@Entity
@Table(name = "booking_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(length = 40)
    private String label;

    @Column(name = "address_line", nullable = false, length = 200)
    private String addressLine;

    @Column(length = 160)
    private String locality;

    @Column(name = "street_name", length = 160)
    private String streetName;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String state;

    @Column(nullable = false, length = 6)
    private String pincode;

    private Double latitude;

    private Double longitude;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
