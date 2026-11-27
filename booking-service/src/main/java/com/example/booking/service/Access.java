package com.example.booking.service;

import com.example.booking.client.CatalogClient;
import com.example.booking.entity.Booking;
import com.example.booking.exception.ForbiddenException;
import com.example.booking.security.AuthPrincipal;
import org.springframework.stereotype.Component;

/**
 * Who may act on a booking. A booking has two participants: the customer who
 * made it and the technician profile it was made with. Admins may read any
 * booking but do not act on it.
 */
@Component
public class Access {

    private final CatalogClient catalog;

    public Access(CatalogClient catalog) {
        this.catalog = catalog;
    }

    public boolean isCustomerOf(Booking booking, AuthPrincipal me) {
        return me.userId().equals(booking.getCustomerId());
    }

    public boolean isTechnicianOf(Booking booking, AuthPrincipal me) {
        if (!me.isTechnician()) {
            return false;
        }
        return catalog.technicianProfileIdFor(me.userId())
                .map(profileId -> profileId.equals(booking.getTechnicianProfileId()))
                .orElse(false);
    }

    public void requireCustomer(Booking booking, AuthPrincipal me) {
        if (!isCustomerOf(booking, me)) {
            throw new ForbiddenException("Only the customer of this booking can do that");
        }
    }

    public void requireTechnician(Booking booking, AuthPrincipal me) {
        if (!isTechnicianOf(booking, me)) {
            throw new ForbiddenException("Only the assigned technician can do that");
        }
    }

    /** Reading a booking: its customer, its technician, or an admin. */
    public void requireParticipant(Booking booking, AuthPrincipal me) {
        if (me.isAdmin() || isCustomerOf(booking, me) || isTechnicianOf(booking, me)) {
            return;
        }
        throw new ForbiddenException("You are not part of this booking");
    }
}
