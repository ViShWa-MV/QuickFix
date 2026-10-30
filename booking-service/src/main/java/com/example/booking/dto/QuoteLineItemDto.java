package com.example.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuoteLineItemDto {

    @NotBlank
    private String description;

    @NotNull
    @Positive
    private Double amount;
}
