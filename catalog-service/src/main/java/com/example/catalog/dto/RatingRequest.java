package com.example.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * One customer star rating to fold into a technician's cached average.
 * Sent by booking-service when a review with a rating is submitted.
 */
@Getter
@Setter
public class RatingRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;
}
