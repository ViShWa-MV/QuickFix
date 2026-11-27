package com.example.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * A customer's review of a finished job. Both fields are optional individually,
 * but at least one must be present (checked in ReviewService). The reviewer is
 * the signed-in customer, taken from the token.
 */
@Getter
@Setter
public class ReviewRequest {

    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 1000)
    private String comment;
}
