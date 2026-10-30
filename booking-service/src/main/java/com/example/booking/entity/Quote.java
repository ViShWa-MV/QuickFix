package com.example.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * An in-job quote for extra parts/labor on a booking. At most one per booking
 * ({@code bookingId} is unique). {@code amount} is the sum of the line items.
 */
@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bookingId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "quote_line_items",
            joinColumns = @JoinColumn(name = "quote_id"))
    @Builder.Default
    private List<QuoteLineItem> lineItems = new ArrayList<>();

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QuoteStatus status = QuoteStatus.PROPOSED;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;
}
