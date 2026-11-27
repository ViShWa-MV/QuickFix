package com.example.booking.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;

/** Writes 401/403 responses in the same JSON shape as the other errors. */
final class SecurityErrors {

    private SecurityErrors() {
    }

    static void write(HttpServletResponse response, HttpServletRequest request,
                      int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"timestamp\":\"" + Instant.now()
                + "\",\"status\":" + status
                + ",\"error\":\"" + error
                + "\",\"message\":\"" + message
                + "\",\"path\":\"" + request.getRequestURI() + "\"}");
    }
}
