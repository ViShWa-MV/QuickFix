package com.example.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Technician's proposed extra charges for a booking. */
@Getter
@Setter
public class QuoteRequest {

    @NotEmpty
    private List<@Valid QuoteLineItemDto> lineItems;
}
