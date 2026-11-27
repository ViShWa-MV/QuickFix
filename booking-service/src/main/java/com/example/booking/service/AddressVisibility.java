package com.example.booking.service;

import com.example.booking.dto.AddressView;
import com.example.booking.entity.BookingAddress;
import com.example.booking.entity.BookingStatus;

import java.util.EnumSet;
import java.util.Set;

/**
 * Who sees the full address of a booking.
 *
 * <ul>
 *   <li>The customer who booked: always.</li>
 *   <li>The assigned technician: once the booking is accepted (CONFIRMED or later).
 *       Before that they see the area only, which is enough to decide.</li>
 *   <li>Anyone else, including admins: the area only.</li>
 * </ul>
 */
public final class AddressVisibility {

    static final Set<BookingStatus> REVEALED_TO_TECHNICIAN = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.EN_ROUTE,
            BookingStatus.IN_PROGRESS,
            BookingStatus.COMPLETED,
            BookingStatus.CLOSED);

    private AddressVisibility() {
    }

    public static AddressView viewFor(BookingAddress a, BookingStatus status,
                                      boolean isCustomer, boolean isAssignedTechnician) {
        boolean full = isCustomer || (isAssignedTechnician && REVEALED_TO_TECHNICIAN.contains(status));
        if (full) {
            return new AddressView("FULL", a.getLocality(), a.getCity(), a.getState(), a.getPincode(),
                    a.getStreetName(), a.getAddressLine(), a.getLatitude(), a.getLongitude());
        }
        return new AddressView("AREA", a.getLocality(), a.getCity(), a.getState(), a.getPincode(),
                null, null, null, null);
    }
}
