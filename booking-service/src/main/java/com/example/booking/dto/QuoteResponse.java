package com.example.booking.dto;

import com.example.booking.entity.Quote;
import com.example.booking.entity.QuoteStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class QuoteResponse {

    private Long id;
    private Long bookingId;
    private List<Line> lineItems;
    private Double amount;
    private QuoteStatus status;
    private LocalDateTime createdAt;

    public record Line(String description, Double amount) {
    }

    public static QuoteResponse from(Quote q) {
        List<Line> lines = q.getLineItems().stream()
                .map(li -> new Line(li.getDescription(), li.getAmount()))
                .toList();

        return new QuoteResponse(
                q.getId(),
                q.getBookingId(),
                lines,
                q.getAmount(),
                q.getStatus(),
                q.getCreatedAt());
    }
}
