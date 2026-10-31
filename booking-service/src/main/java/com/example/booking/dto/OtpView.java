package com.example.booking.dto;

import com.example.booking.entity.BookingOtp;
import com.example.booking.entity.OtpPurpose;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * The active code as shown to the customer (customer-facing only; the
 * technician's responses never include the code).
 */
@Getter
@AllArgsConstructor
public class OtpView {

    private OtpPurpose purpose;
    private String code;
    private LocalDateTime expiresAt;
    private int attemptsRemaining;

    public static OtpView from(BookingOtp o) {
        return new OtpView(
                o.getPurpose(),
                o.getCode(),
                o.getExpiresAt(),
                Math.max(0, o.getMaxAttempts() - o.getAttempts()));
    }
}
