package com.example.booking.service;

import com.example.booking.client.CatalogClient;
import com.example.booking.dto.BookingRequest;
import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.exception.ForbiddenException;
import com.example.booking.exception.SlotUnavailableException;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.QuoteRepository;
import com.example.booking.security.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository repository;
    @Mock QuoteRepository quoteRepository;
    @Mock com.example.booking.repository.ReviewRepository reviewRepository;
    @Mock com.example.booking.repository.BookingAddressRepository addressRepository;
    @Mock com.example.booking.client.UserServiceClient userService;
    @Mock com.example.booking.repository.BookingOtpRepository otpRepository;
    @Mock OtpService otpService;
    @Mock CatalogClient catalog;

    private BookingService service;

    private static final AuthPrincipal ASSIGNED_TECHNICIAN = new AuthPrincipal(4L, "TECHNICIAN");
    private static final AuthPrincipal OTHER_TECHNICIAN = new AuthPrincipal(8L, "TECHNICIAN");

    @BeforeEach
    void setUp() {
        service = new BookingService(repository, quoteRepository, reviewRepository, otpRepository, addressRepository,
                otpService, catalog, userService, new Access(catalog));
    }

    private Booking requested(LocalDateTime expiresAt) {
        return Booking.builder()
                .id(1L)
                .customerId(5L)
                .technicianProfileId(7L)
                .slotId(9L)
                .status(BookingStatus.REQUESTED)
                .requestedAt(LocalDateTime.now().minusMinutes(20))
                .expiresAt(expiresAt)
                .build();
    }

    @Test
    void acceptingAnExpiredRequestMarksItExpiredAndFreesTheSlot() {
        Booking booking = requested(LocalDateTime.now().minusMinutes(1));
        when(repository.findById(1L)).thenReturn(Optional.of(booking));
        when(catalog.technicianProfileIdFor(4L)).thenReturn(Optional.of(7L));

        assertThatThrownBy(() -> service.accept(ASSIGNED_TECHNICIAN, 1L))
                .isInstanceOf(SlotUnavailableException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(repository).save(booking);
        verify(catalog).releaseSlotQuietly(9L);
        verify(catalog, never()).bookSlot(anyLong());
    }

    @Test
    void theExpiredStatusIsNotRolledBackWhenAcceptThrows() throws NoSuchMethodException {
        Transactional tx = BookingService.class
                .getMethod("accept", AuthPrincipal.class, Long.class)
                .getAnnotation(Transactional.class);

        // Without this, the EXPIRED save is undone along with the exception.
        assertThat(tx.noRollbackFor()).contains(SlotUnavailableException.class);
    }

    @Test
    void anotherTechnicianCannotAcceptABooking() {
        Booking booking = requested(LocalDateTime.now().plusMinutes(10));
        when(repository.findById(1L)).thenReturn(Optional.of(booking));
        when(catalog.technicianProfileIdFor(8L)).thenReturn(Optional.of(99L));

        assertThatThrownBy(() -> service.accept(OTHER_TECHNICIAN, 1L))
                .isInstanceOf(ForbiddenException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REQUESTED);
        verify(catalog, never()).bookSlot(anyLong());
    }

    @Test
    void aCustomerCannotReadTheStartCodeOfAnotherCustomersBooking() {
        Booking booking = requested(LocalDateTime.now().plusMinutes(10));
        when(repository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getActiveOtp(
                new AuthPrincipal(6L, "CUSTOMER"), 1L,
                com.example.booking.entity.OtpPurpose.START))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void aTechnicianCannotReadTheStartCode() {
        Booking booking = requested(LocalDateTime.now().plusMinutes(10));
        when(repository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getActiveOtp(
                ASSIGNED_TECHNICIAN, 1L,
                com.example.booking.entity.OtpPurpose.START))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void aBookingIsNotCreatedWithAnUnapprovedTechnicianAndNoSlotIsHeld() {
        when(catalog.getServicePrice(3L)).thenReturn(499.0);
        when(catalog.getTechnician(7L)).thenReturn(new CatalogClient.TechnicianView(7L, 4L, "PENDING"));

        BookingRequest request = new BookingRequest();
        request.setTechnicianProfileId(7L);
        request.setServiceId(3L);
        request.setSlotId(9L);
        request.setAddressId(100L);
        when(userService.getAddress(100L)).thenReturn(new com.example.booking.client.UserServiceClient.AddressSnapshot(
                100L, 5L, "Home", "Flat 4B", "Central", "MG Road", "Bengaluru", "Karnataka", "560001", 12.97, 77.6, "VERIFIED"));

        assertThatThrownBy(() -> service.create(new AuthPrincipal(5L, "CUSTOMER"), request))
                .isInstanceOf(IllegalStateException.class);

        verify(catalog, never()).holdSlot(any(), any());
        verify(repository, never()).save(any());
    }
}
