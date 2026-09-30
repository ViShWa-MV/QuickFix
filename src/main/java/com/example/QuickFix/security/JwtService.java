package com.example.QuickFix.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes()
        );

        this.expiration = expiration;
    }

    // Generate JWT
    public String generateToken(
            Long userId,
            String email,
            String role) {

        Date now = new Date();

        Date expiryDate = new Date(
                now.getTime() + expiration
        );

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    // Extract all claims from JWT
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extract user ID
    public Long extractUserId(String token) {

        String subject = extractAllClaims(token)
                .getSubject();

        return Long.parseLong(subject);
    }

    // Extract role
    public String extractRole(String token) {

        return extractAllClaims(token)
                .get("role", String.class);
    }

    // Extract email
    public String extractEmail(String token) {

        return extractAllClaims(token)
                .get("email", String.class);
    }

    // Validate JWT
    public boolean isTokenValid(String token) {

        try {

            extractAllClaims(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}