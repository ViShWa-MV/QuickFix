package com.example.booking.service;

import com.example.booking.entity.BookingOtp;
import com.example.booking.entity.OtpPurpose;
import com.example.booking.entity.OtpStatus;
import com.example.booking.exception.OtpException;
import com.example.booking.repository.BookingOtpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private static final long EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    private final BookingOtpRepository repository;
    private final SecureRandom random = new SecureRandom();

    public OtpService(BookingOtpRepository repository) {
        this.repository = repository;
    }

    /** Issue a fresh code, voiding any previous active one for this purpose. */
    @Transactional
    public BookingOtp generate(Long bookingId, OtpPurpose purpose) {
        repository.findFirstByBookingIdAndPurposeAndStatus(
                bookingId, purpose, OtpStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(OtpStatus.VOID);
                    repository.save(existing);
                });

        LocalDateTime now = LocalDateTime.now();
        BookingOtp otp = BookingOtp.builder()
                .bookingId(bookingId)
                .purpose(purpose)
                .code(String.format("%06d", random.nextInt(1_000_000)))
                .status(OtpStatus.ACTIVE)
                .attempts(0)
                .maxAttempts(MAX_ATTEMPTS)
                .createdAt(now)
                .expiresAt(now.plusMinutes(EXPIRY_MINUTES))
                .build();

        return repository.save(otp);
    }

    /**
     * Verify a submitted code. On success the OTP is consumed (VERIFIED).
     * Wrong-but-usable -> OtpException (400). Missing/expired/exhausted ->
     * IllegalStateException (409, "resend").
     *
     * noRollbackFor: a wrong attempt increments the counter (and an expiry flip
     * marks EXPIRED) and those must COMMIT even though we then signal an error,
     * so the attempt lock can trip. Matches the callers' rollback rules so the
     * shared transaction isn't marked rollback-only.
     */
    @Transactional(noRollbackFor = { OtpException.class, IllegalStateException.class })
    public void verify(Long bookingId, OtpPurpose purpose, String submitted) {
        BookingOtp otp = repository
                .findFirstByBookingIdAndPurposeAndStatus(
                        bookingId, purpose, OtpStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException(
                        "No active code — ask for it to be re-sent"));

        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            otp.setStatus(OtpStatus.EXPIRED);
            repository.save(otp);
            throw new IllegalStateException("Code expired — ask for a new one");
        }
        if (otp.getAttempts() >= otp.getMaxAttempts()) {
            throw new IllegalStateException(
                    "Too many attempts — ask for a new code");
        }

        if (!constantTimeEquals(otp.getCode(), submitted)) {
            otp.setAttempts(otp.getAttempts() + 1);
            repository.save(otp);
            int left = otp.getMaxAttempts() - otp.getAttempts();
            throw new OtpException(left > 0
                    ? "Incorrect code — " + left + " attempt(s) left"
                    : "Incorrect code — ask for a new one");
        }

        otp.setStatus(OtpStatus.VERIFIED);
        otp.setVerifiedAt(LocalDateTime.now());
        repository.save(otp);
    }

    /** The active, unexpired code for the customer to read (lazily expires). */
    @Transactional
    public Optional<BookingOtp> activeFor(Long bookingId, OtpPurpose purpose) {
        Optional<BookingOtp> found = repository
                .findFirstByBookingIdAndPurposeAndStatus(
                        bookingId, purpose, OtpStatus.ACTIVE);
        if (found.isPresent()
                && LocalDateTime.now().isAfter(found.get().getExpiresAt())) {
            BookingOtp o = found.get();
            o.setStatus(OtpStatus.EXPIRED);
            repository.save(o);
            return Optional.empty();
        }
        return found;
    }

    /** Void every active code for a booking (e.g. on cancellation). */
    @Transactional
    public void voidAll(Long bookingId) {
        repository.findByBookingIdAndStatus(bookingId, OtpStatus.ACTIVE)
                .forEach(o -> {
                    o.setStatus(OtpStatus.VOID);
                    repository.save(o);
                });
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
