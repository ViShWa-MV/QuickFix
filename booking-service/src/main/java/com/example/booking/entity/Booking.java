package com.example.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A customer's booking of a technician for a service in a specific slot.
 *
 * <p>All references (customer, technician profile, service, slot) are logical
 * ids into user-service / catalog-service — no cross-service foreign keys.
 * {@code baseAmount} is snapshotted from the service's price at request time.
 * {@code version} gives optimistic locking so two concurrent transitions on the
 * same booking can't both win.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long technicianProfileId;

    @Column(nullable = false)
    private Long serviceId;

    @Column(nullable = false)
    private Long slotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    private Double baseAmount;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime expiresAt;

    @Version
    private Long version;
}
