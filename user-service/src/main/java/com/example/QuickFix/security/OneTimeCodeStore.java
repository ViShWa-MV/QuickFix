package com.example.QuickFix.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hands a freshly issued JWT to the browser without putting it in a URL.
 *
 * <p>After Google sign-in the redirect carries a random, single-use code. The
 * SPA exchanges that code for the token with a POST, so the token never appears
 * in browser history, server access logs, or Referer headers.
 */
@Component
public class OneTimeCodeStore {

    private static final Duration TTL = Duration.ofSeconds(60);

    private record Entry(String token, Instant expiresAt) {
    }

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /** Store a token and return the one-time code that redeems it. */
    public String issue(String token) {
        purgeExpired();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        codes.put(code, new Entry(token, Instant.now().plus(TTL)));
        return code;
    }

    /** Redeem a code. Works once: the code is removed whether or not it is still valid. */
    public Optional<String> redeem(String code) {
        if (code == null) {
            return Optional.empty();
        }
        Entry entry = codes.remove(code);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(entry.token());
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        codes.values().removeIf(e -> now.isAfter(e.expiresAt()));
    }
}
