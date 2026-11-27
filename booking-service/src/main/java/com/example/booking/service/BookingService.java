package com.example.booking.service;

import com.example.booking.client.CatalogClient;
import com.example.booking.client.UserServiceClient;
import com.example.booking.dto.BookingRequest;
import com.example.booking.dto.AddressView;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.BookingSummaryResponse;
import com.example.booking.dto.QuoteResponse;
import com.example.booking.dto.ReviewResponse;
import com.example.booking.dto.OtpView;
import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingAddress;
import com.example.booking.entity.BookingOtp;
import com.example.booking.entity.BookingStatus;
import com.example.booking.entity.OtpStatus;
import com.example.booking.entity.Quote;
import com.example.booking.entity.Review;
import com.example.booking.entity.OtpPurpose;
import com.example.booking.entity.QuoteStatus;
import com.example.booking.exception.ForbiddenException;
import com.example.booking.exception.OtpException;
import com.example.booking.exception.ForbiddenException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.exception.SlotUnavailableException;
import com.example.booking.repository.BookingAddressRepository;
import com.example.booking.repository.BookingOtpRepository;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.ReviewRepository;
import com.example.booking.repository.QuoteRepository;
import com.example.booking.security.AuthPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Booking lifecycle. Every method takes the caller and checks that they may do
 * the action: customers act on their own bookings, technicians on the bookings
 * assigned to their profile.
 */
@Service
public class BookingService {

    /** How long a REQUESTED booking waits for the technician to accept. */
    private static final long REQUEST_TTL_MINUTES = 15;

    private final BookingRepository repository;
    private final QuoteRepository quoteRepository;
    private final ReviewRepository reviewRepository;
    private final BookingOtpRepository otpRepository;
    private final BookingAddressRepository addressRepository;
    private final OtpService otpService;
    private final CatalogClient catalog;
    private final UserServiceClient userService;
    private final Access access;

    public BookingService(
            BookingRepository repository,
            QuoteRepository quoteRepository,
            ReviewRepository reviewRepository,
            BookingOtpRepository otpRepository,
            BookingAddressRepository addressRepository,
            OtpService otpService,
            CatalogClient catalog,
            UserServiceClient userService,
            Access access) {
        this.repository = repository;
        this.quoteRepository = quoteRepository;
        this.reviewRepository = reviewRepository;
        this.otpRepository = otpRepository;
        this.addressRepository = addressRepository;
        this.otpService = otpService;
        this.catalog = catalog;
        this.userService = userService;
        this.access = access;
    }

    // =====================================================================
    // Create (a customer requests a technician for one of their slots)
    // =====================================================================

    @Transactional
    public BookingResponse create(AuthPrincipal me, BookingRequest request) {

        // 0. The address must be the customer's own, and must be verified.
        UserServiceClient.AddressSnapshot address = userService.getAddress(request.getAddressId());
        if (!me.userId().equals(address.ownerUserId())) {
            throw new ForbiddenException("That address is not yours");
        }
        if (!"VERIFIED".equals(address.verificationStatus())) {
            throw new IllegalStateException(
                    "This address is not verified. Save it again from your profile using a street from the suggestions.");
        }

        // 1. Snapshot the price (also checks the service exists).
        double baseAmount = catalog.getServicePrice(request.getServiceId());

        // 2. Only approved technicians take bookings.
        CatalogClient.TechnicianView technician =
                catalog.getTechnician(request.getTechnicianProfileId());
        if (!"APPROVED".equals(technician.verificationStatus())) {
            throw new IllegalStateException("This technician is not taking bookings");
        }

        // 3. Hold the slot. This is the double-booking guard; catalog refuses
        //    a slot that another technician publishes.
        Long slotId = request.getSlotId();
        catalog.holdSlot(slotId, request.getTechnicianProfileId());

        // 4. If this transaction rolls back after the hold, give the slot back
        //    now instead of waiting for the sweep.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            catalog.releaseSlotQuietly(slotId);
                        }
                    }
                });

        LocalDateTime now = LocalDateTime.now();
        Booking booking = Booking.builder()
                .customerId(me.userId())
                .technicianProfileId(request.getTechnicianProfileId())
                .serviceId(request.getServiceId())
                .slotId(slotId)
                .status(BookingStatus.REQUESTED)
                .baseAmount(baseAmount)
                .requestedAt(now)
                .expiresAt(now.plusMinutes(REQUEST_TTL_MINUTES))
                .build();

        Booking saved = repository.save(booking);
        addressRepository.save(BookingAddress.builder()
                .bookingId(saved.getId())
                .customerId(me.userId())
                .label(address.label())
                .addressLine(address.addressLine())
                .locality(address.locality())
                .streetName(address.streetName())
                .city(address.city())
                .state(address.state())
                .pincode(address.pincode())
                .latitude(address.latitude())
                .longitude(address.longitude())
                .createdAt(now)
                .build());

        return BookingResponse.from(saved);
    }

    // =====================================================================
    // Technician decisions
    // =====================================================================

    /**
     * Technician accepts: book the slot, then confirm. The noRollbackFor keeps
     * the EXPIRED status saved when the request has already timed out.
     */
    @Transactional(noRollbackFor = SlotUnavailableException.class)
    public BookingResponse accept(AuthPrincipal me, Long id) {

        Booking booking = find(id);
        access.requireTechnician(booking, me);
        requireStatus(booking, BookingStatus.REQUESTED);

        if (isExpired(booking)) {
            booking.setStatus(BookingStatus.EXPIRED);
            repository.save(booking);
            catalog.releaseSlotQuietly(booking.getSlotId());
            throw new SlotUnavailableException("This request has expired");
        }

        try {
            // HELD -> BOOKED before confirming, so we never confirm a booking
            // whose slot we could not lock.
            catalog.bookSlot(booking.getSlotId());
        } catch (SlotUnavailableException ex) {
            booking.setStatus(BookingStatus.EXPIRED);
            repository.save(booking);
            throw ex;
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return BookingResponse.from(repository.save(booking));
    }

    @Transactional
    public BookingResponse decline(AuthPrincipal me, Long id) {
        Booking booking = find(id);
        access.requireTechnician(booking, me);
        requireStatus(booking, BookingStatus.REQUESTED);

        booking.setStatus(BookingStatus.DECLINED);
        BookingResponse response = BookingResponse.from(repository.save(booking));

        catalog.releaseSlotQuietly(booking.getSlotId());
        return response;
    }

    // =====================================================================
    // Shared lifecycle
    // =====================================================================

    /** Either party may cancel before work completes. */
    @Transactional
    public BookingResponse cancel(AuthPrincipal me, Long id) {
        Booking booking = find(id);
        if (!access.isCustomerOf(booking, me) && !access.isTechnicianOf(booking, me)) {
            throw new ForbiddenException("Only the customer or the technician can cancel");
        }
        requireStatusIn(booking,
                BookingStatus.REQUESTED,
                BookingStatus.CONFIRMED,
                BookingStatus.EN_ROUTE);

        booking.setStatus(BookingStatus.CANCELLED);
        BookingResponse response = BookingResponse.from(repository.save(booking));

        otpService.voidAll(id);
        catalog.releaseSlotQuietly(booking.getSlotId());
        return response;
    }

    /** Customer only: the active code to read out to the technician. */
    public OtpView getActiveOtp(AuthPrincipal me, Long id, OtpPurpose purpose) {
        Booking booking = find(id);
        access.requireCustomer(booking, me);
        return otpService.activeFor(id, purpose)
                .map(OtpView::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active code for this booking"));
    }

    @Transactional
    public BookingResponse markEnRoute(AuthPrincipal me, Long id) {
        return move(me, id, BookingStatus.CONFIRMED, BookingStatus.EN_ROUTE);
    }

    /** Technician reached the address: issue the start code to the customer. */
    @Transactional
    public BookingResponse arrive(AuthPrincipal me, Long id) {
        Booking booking = find(id);
        access.requireTechnician(booking, me);
        requireStatus(booking, BookingStatus.EN_ROUTE);
        otpService.generate(id, OtpPurpose.START);
        return BookingResponse.from(booking);
    }

    /**
     * Start work with the customer's start code. A wrong code is still saved (its
     * attempt is counted), so noRollbackFor is required.
     */
    @Transactional(noRollbackFor = { OtpException.class, IllegalStateException.class })
    public BookingResponse start(AuthPrincipal me, Long id, String otp) {
        Booking booking = find(id);
        access.requireTechnician(booking, me);
        requireStatus(booking, BookingStatus.EN_ROUTE);
        otpService.verify(id, OtpPurpose.START, otp);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        return BookingResponse.from(repository.save(booking));
    }

    /** Work finished: issue the completion code to the customer. */
    @Transactional
    public BookingResponse requestCompletion(AuthPrincipal me, Long id) {
        Booking booking = find(id);
        access.requireTechnician(booking, me);
        requireStatus(booking, BookingStatus.IN_PROGRESS);
        otpService.generate(id, OtpPurpose.COMPLETION);
        return BookingResponse.from(booking);
    }

    /** Complete: needs no pending quote and the customer's completion code. */
    @Transactional(noRollbackFor = { OtpException.class, IllegalStateException.class })
    public BookingResponse complete(AuthPrincipal me, Long id, String otp) {
        Booking booking = find(id);
        access.requireTechnician(booking, me);
        requireStatus(booking, BookingStatus.IN_PROGRESS);

        // Checked before the code is consumed, so a pending quote doesn't burn it.
        quoteRepository.findByBookingId(id).ifPresent(quote -> {
            if (quote.getStatus() == QuoteStatus.PROPOSED) {
                throw new IllegalStateException(
                        "Resolve the pending quote before completing the job");
            }
        });

        otpService.verify(id, OtpPurpose.COMPLETION, otp);
        booking.setStatus(BookingStatus.COMPLETED);
        return BookingResponse.from(repository.save(booking));
    }

    @Transactional
    public BookingResponse close(AuthPrincipal me, Long id) {
        return move(me, id, BookingStatus.COMPLETED, BookingStatus.CLOSED);
    }

    // =====================================================================
    // Reads
    // =====================================================================

    public BookingResponse getById(AuthPrincipal me, Long id) {
        Booking booking = find(id);
        access.requireParticipant(booking, me);
        return BookingResponse.from(booking);
    }

    /**
     * The address of one booking, as this caller may see it. Participants only:
     * the customer, the assigned technician, or an admin (who sees the area only).
     */
    public AddressView addressFor(AuthPrincipal me, Long bookingId) {
        Booking booking = find(bookingId);
        access.requireParticipant(booking, me);
        BookingAddress address = addressRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No address for this booking"));
        return AddressVisibility.viewFor(address, booking.getStatus(),
                access.isCustomerOf(booking, me), access.isTechnicianOf(booking, me));
    }

    /** A customer's own bookings (or any, for an admin). Paged. */
    public List<BookingResponse> getForCustomer(AuthPrincipal me, Long customerId, Pageable pageable) {
        requireCustomerView(me, customerId);
        return repository.findByCustomerIdOrderByRequestedAtDesc(customerId, pageable)
                .stream().map(BookingResponse::from).toList();
    }

    /** A technician profile's bookings: its own owner, or an admin. Paged. */
    public List<BookingResponse> getForTechnician(AuthPrincipal me, Long technicianProfileId, Pageable pageable) {
        requireTechnicianView(me, technicianProfileId);
        return repository.findByTechnicianProfileIdOrderByRequestedAtDesc(technicianProfileId, pageable)
                .stream().map(BookingResponse::from).toList();
    }

    /**
     * The customer's bookings with quote, active code, and review in one response.
     * The active code is included only where the customer may see it.
     */
    public List<BookingSummaryResponse> customerSummary(AuthPrincipal me, Long customerId, Pageable pageable) {
        requireCustomerView(me, customerId);
        return summarize(repository.findByCustomerIdOrderByRequestedAtDesc(customerId, pageable), true,
                me.userId().equals(customerId), false);
    }

    /** The technician's jobs with quote and review in one response (no codes: technicians never see them). */
    public List<BookingSummaryResponse> technicianSummary(AuthPrincipal me, Long technicianProfileId, Pageable pageable) {
        requireTechnicianView(me, technicianProfileId);
        // Admins read these jobs too, but are not the assigned technician.
        return summarize(repository.findByTechnicianProfileIdOrderByRequestedAtDesc(technicianProfileId, pageable), false,
                false, !me.isAdmin());
    }

    private void requireCustomerView(AuthPrincipal me, Long customerId) {
        if (!me.isAdmin() && !me.userId().equals(customerId)) {
            throw new ForbiddenException("You can only view your own bookings");
        }
    }

    private void requireTechnicianView(AuthPrincipal me, Long technicianProfileId) {
        boolean owner = me.isTechnician() && catalog.technicianProfileIdFor(me.userId())
                .map(technicianProfileId::equals).orElse(false);
        if (!me.isAdmin() && !owner) {
            throw new ForbiddenException("You can only view your own jobs");
        }
    }

    /** Builds summaries with one query per related table, not one per booking. */
    private List<BookingSummaryResponse> summarize(List<Booking> bookings, boolean includeCodes,
                                                   boolean viewerIsCustomer, boolean viewerIsTechnician) {
        if (bookings.isEmpty()) {
            return List.of();
        }
        List<Long> ids = bookings.stream().map(Booking::getId).toList();

        Map<Long, BookingAddress> addresses = addressRepository.findByBookingIdIn(ids).stream()
                .collect(Collectors.toMap(BookingAddress::getBookingId, a -> a, (a, b) -> a));

        Map<Long, QuoteResponse> quotes = quoteRepository.findByBookingIdIn(ids).stream()
                .collect(Collectors.toMap(Quote::getBookingId, QuoteResponse::from, (a, b) -> a));
        Map<Long, ReviewResponse> reviews = reviewRepository.findByBookingIdIn(ids).stream()
                .collect(Collectors.toMap(Review::getBookingId, ReviewResponse::from, (a, b) -> a));

        // Only the code for the current milestone, and only while it is still valid.
        LocalDateTime now = LocalDateTime.now();
        Map<Long, BookingStatus> statusById = bookings.stream()
                .collect(Collectors.toMap(Booking::getId, Booking::getStatus));
        Map<Long, OtpView> codes = includeCodes
                ? otpRepository.findByBookingIdInAndStatus(ids, OtpStatus.ACTIVE).stream()
                        .filter(o -> o.getExpiresAt().isAfter(now))
                        .filter(o -> o.getPurpose() == purposeFor(statusById.get(o.getBookingId())))
                        .collect(Collectors.toMap(BookingOtp::getBookingId, OtpView::from, (a, b) -> a))
                : Map.of();

        return bookings.stream()
                .map(b -> BookingSummaryResponse.of(
                        b,
                        quotes.get(b.getId()),
                        codes.get(b.getId()),
                        reviews.get(b.getId()),
                        addresses.containsKey(b.getId())
                                ? AddressVisibility.viewFor(addresses.get(b.getId()), b.getStatus(),
                                        viewerIsCustomer, viewerIsTechnician)
                                : null))
                .toList();
    }

    /** The code a booking needs right now: START while on the way, COMPLETION while working. */
    private static OtpPurpose purposeFor(BookingStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case EN_ROUTE -> OtpPurpose.START;
            case IN_PROGRESS -> OtpPurpose.COMPLETION;
            default -> null;
        };
    }

    // =====================================================================
    // Expiry sweep (called by the scheduler, no caller)
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

    private BookingResponse move(AuthPrincipal me, Long id,
                                 BookingStatus required, BookingStatus next) {
        Booking booking = find(id);
        access.requireTechnician(booking, me);
        requireStatus(booking, required);
        booking.setStatus(next);
        return BookingResponse.from(repository.save(booking));
    }

    private Booking find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private void requireStatus(Booking booking, BookingStatus expected) {
        if (booking.getStatus() != expected) {
            throw new IllegalStateException(
                    "Booking must be " + expected + " but is " + booking.getStatus());
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
