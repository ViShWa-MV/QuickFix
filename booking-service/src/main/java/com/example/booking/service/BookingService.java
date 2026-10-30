package com.example.booking.service;

import com.example.booking.client.CatalogClient;
import com.example.booking.dto.BookingRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.exception.SlotUnavailableException;
import com.example.booking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    /** How long a REQUESTED booking waits for the technician to accept. */
    private static final long REQUEST_TTL_MINUTES = 15;

    private final BookingRepository repository;
    private final CatalogClient catalog;

    public BookingService(
            BookingRepository repository,
            CatalogClient catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    // =====================================================================
    // Create (customer requests a technician for a slot)
    // =====================================================================

    @Transactional
    public BookingResponse create(BookingRequest request) {

        // 1. Snapshot the service price (also validates the service exists).
        double baseAmount = catalog.getServicePrice(request.getServiceId());

        // 2. Hold the slot FIRST — this is the atomic guard against
        //    double-booking. Throws if it is not OPEN / does not exist.
        catalog.holdSlot(request.getSlotId());

        // 3. Create the booking. If persistence fails, compensate by
        //    releasing the slot we just held.
        try {
            LocalDateTime now = LocalDateTime.now();

            Booking booking = Booking.builder()
                    .customerId(request.getCustomerId())
                    .technicianProfileId(request.getTechnicianProfileId())
                    .serviceId(request.getServiceId())
                    .slotId(request.getSlotId())
                    .status(BookingStatus.REQUESTED)
                    .baseAmount(baseAmount)
                    .requestedAt(now)
                    .expiresAt(now.plusMinutes(REQUEST_TTL_MINUTES))
                    .build();

            return BookingResponse.from(repository.save(booking));

        } catch (RuntimeException ex) {
            catalog.releaseSlotQuietly(request.getSlotId());
            throw ex;
        }
    }

    // =====================================================================
    // Technician decisions
    // =====================================================================

    /** Technician accepts: book the slot, then confirm. */
    @Transactional
    public BookingResponse accept(Long id) {

        Booking booking = find(id);
        requireStatus(booking, BookingStatus.REQUESTED);

        if (isExpired(booking)) {
            booking.setStatus(BookingStatus.EXPIRED);
            repository.save(booking);
            catalog.releaseSlotQuietly(booking.getSlotId());
            throw new SlotUnavailableException(
                    "This request has expired");
        }

        try {
            // HELD -> BOOKED. Book BEFORE confirming, so we never confirm a
            // booking whose slot we could not lock.
            catalog.bookSlot(booking.getSlotId());
        } catch (SlotUnavailableException ex) {
            // The slot was swept/released out from under us; reconcile.
            booking.setStatus(BookingStatus.EXPIRED);
            repository.save(booking);
            throw ex;
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return BookingResponse.from(repository.save(booking));
    }

    /** Technician declines a pending request. */
    @Transactional
    public BookingResponse decline(Long id) {
        Booking booking = find(id);
        requireStatus(booking, BookingStatus.REQUESTED);

        booking.setStatus(BookingStatus.DECLINED);
        BookingResponse response = BookingResponse.from(repository.save(booking));

        catalog.releaseSlotQuietly(booking.getSlotId());
        return response;
    }

    // =====================================================================
    // Shared lifecycle
    // =====================================================================

    /** Either party cancels, per policy, before work completes. */
    @Transactional
    public BookingResponse cancel(Long id) {
        Booking booking = find(id);
        requireStatusIn(booking,
                BookingStatus.REQUESTED,
                BookingStatus.CONFIRMED,
                BookingStatus.EN_ROUTE);

        booking.setStatus(BookingStatus.CANCELLED);
        BookingResponse response = BookingResponse.from(repository.save(booking));

        catalog.releaseSlotQuietly(booking.getSlotId());
        return response;
    }

    @Transactional
    public BookingResponse markEnRoute(Long id) {
        return move(id, BookingStatus.CONFIRMED, BookingStatus.EN_ROUTE);
    }

    @Transactional
    public BookingResponse start(Long id) {
        return move(id, BookingStatus.EN_ROUTE, BookingStatus.IN_PROGRESS);
    }

    @Transactional
    public BookingResponse complete(Long id) {
        return move(id, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED);
    }

    @Transactional
    public BookingResponse close(Long id) {
        return move(id, BookingStatus.COMPLETED, BookingStatus.CLOSED);
    }

    // =====================================================================
    // Reads
    // =====================================================================

    public BookingResponse getById(Long id) {
        return BookingResponse.from(find(id));
    }

    public List<BookingResponse> getForCustomer(Long customerId) {
        return repository.findByCustomerIdOrderByRequestedAtDesc(customerId)
                .stream().map(BookingResponse::from).toList();
    }

    public List<BookingResponse> getForTechnician(Long technicianProfileId) {
        return repository
                .findByTechnicianProfileIdOrderByRequestedAtDesc(technicianProfileId)
                .stream().map(BookingResponse::from).toList();
    }

    // =====================================================================
    // Expiry sweep (called by the scheduler)
    // =====================================================================

    @Transactional
    public int expireStaleRequests() {
        List<Booking> stale = repository.findByStatusAndExpiresAtBefore(
                BookingStatus.REQUESTED, LocalDateTime.now());

        for (Booking booking : stale) {
            booking.setStatus(BookingStatus.EXPIRED);
            repository.save(booking);
            catalog.releaseSlotQuietly(booking.getSlotId());
        }
        return stale.size();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private BookingResponse move(
            Long id, BookingStatus required, BookingStatus next) {
        Booking booking = find(id);
        requireStatus(booking, required);
        booking.setStatus(next);
        return BookingResponse.from(repository.save(booking));
    }

    private Booking find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found"));
    }

    private void requireStatus(Booking booking, BookingStatus expected) {
        if (booking.getStatus() != expected) {
            throw new IllegalStateException(
                    "Booking must be " + expected
                            + " but is " + booking.getStatus());
        }
    }

    private void requireStatusIn(Booking booking, BookingStatus... allowed) {
        for (BookingStatus s : allowed) {
            if (booking.getStatus() == s) {
                return;
            }
        }
        throw new IllegalStateException(
                "Action not allowed from status " + booking.getStatus());
    }

    private boolean isExpired(Booking booking) {
        return booking.getExpiresAt() != null
                && booking.getExpiresAt().isBefore(LocalDateTime.now());
    }
}
