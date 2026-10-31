package com.example.booking.repository;

import com.example.booking.entity.BookingOtp;
import com.example.booking.entity.OtpPurpose;
import com.example.booking.entity.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingOtpRepository extends JpaRepository<BookingOtp, Long> {

    Optional<BookingOtp> findFirstByBookingIdAndPurposeAndStatus(
            Long bookingId, OtpPurpose purpose, OtpStatus status);

    List<BookingOtp> findByBookingIdAndStatus(Long bookingId, OtpStatus status);
}
