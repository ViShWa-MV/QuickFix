package com.example.booking.service;

import com.example.booking.dto.AddressView;
import com.example.booking.entity.BookingAddress;
import com.example.booking.entity.BookingStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressVisibilityTest {

    private static BookingAddress address() {
        return BookingAddress.builder()
                .bookingId(1L).customerId(5L).addressLine("Flat 4B").locality("Central Bengaluru")
                .streetName("MG Road").city("Bengaluru").state("Karnataka").pincode("560001")
                .latitude(12.9756).longitude(77.6066).build();
    }

    @Test
    void theCustomerAlwaysSeesEverything() {
        for (BookingStatus s : BookingStatus.values()) {
            AddressView v = AddressVisibility.viewFor(address(), s, true, false);
            assertThat(v.visibility()).isEqualTo("FULL");
            assertThat(v.streetName()).isEqualTo("MG Road");
        }
    }

    @Test
    void theTechnicianSeesTheAreaUntilTheBookingIsAccepted() {
        for (BookingStatus s : new BookingStatus[]{BookingStatus.REQUESTED, BookingStatus.DECLINED,
                BookingStatus.EXPIRED, BookingStatus.CANCELLED}) {
            AddressView v = AddressVisibility.viewFor(address(), s, false, true);
            assertThat(v.visibility()).as(s.name()).isEqualTo("AREA");
            assertThat(v.streetName()).isNull();
            assertThat(v.latitude()).isNull();
            assertThat(v.city()).isEqualTo("Bengaluru");
        }
    }

    @Test
    void theTechnicianSeesTheFullAddressFromAcceptOnwards() {
        for (BookingStatus s : new BookingStatus[]{BookingStatus.CONFIRMED, BookingStatus.EN_ROUTE,
                BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED, BookingStatus.CLOSED}) {
            assertThat(AddressVisibility.viewFor(address(), s, false, true).visibility()).as(s.name()).isEqualTo("FULL");
        }
    }

    @Test
    void anyoneElseSeesTheAreaOnly() {
        AddressView v = AddressVisibility.viewFor(address(), BookingStatus.CLOSED, false, false);
        assertThat(v.visibility()).isEqualTo("AREA");
        assertThat(v.addressLine()).isNull();
    }
}
