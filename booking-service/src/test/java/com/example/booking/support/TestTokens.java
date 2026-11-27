package com.example.booking.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Signs JWTs the way user-service does, for calling the API in tests. */
public final class TestTokens {

    public static final String SECRET = "test-only-jwt-secret-at-least-thirty-two-bytes-long";

    private TestTokens() {
    }

    public static String of(long userId, String role) {
        return "Bearer " + Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
