package com.example.booking.security;

/**
 * The caller, taken from a verified JWT (user id and role). Controllers receive
 * this instead of trusting ids in the request body.
 */
public record AuthPrincipal(Long userId, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isTechnician() {
        return "TECHNICIAN".equals(role);
    }
}
