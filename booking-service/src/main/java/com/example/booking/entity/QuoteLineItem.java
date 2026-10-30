package com.example.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/** One line of a quote: a described charge. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteLineItem {

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double amount;
}
