package com.example.booking.dto;

import java.util.List;

/** A technician's most recent reviews, for a results page. */
public record TechnicianReviews(Long technicianProfileId, List<ReviewResponse> reviews) {
}
