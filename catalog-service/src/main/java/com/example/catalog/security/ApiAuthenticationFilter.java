package com.example.catalog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates each request from one of two credentials:
 * <ul>
 *   <li>{@code Authorization: Bearer <jwt>} from a signed-in user, which sets
 *       the principal and a {@code ROLE_*} authority;</li>
 *   <li>{@code X-Internal-Key} from another QuickFix service, which grants the
 *       {@code INTERNAL} authority.</li>
 * </ul>
 * A missing or invalid credential leaves the request anonymous. Tokens and keys
 * are never logged.
 */
@Component
public class ApiAuthenticationFilter extends OncePerRequestFilter {

    static final String INTERNAL_HEADER = "X-Internal-Key";

    private final JwtVerifier jwtVerifier;
    private final byte[] internalKey;

    public ApiAuthenticationFilter(
            JwtVerifier jwtVerifier,
            @Value("${quickfix.internal-key:}") String internalKey) {
        this.jwtVerifier = jwtVerifier;
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            Optional<AuthPrincipal> me = jwtVerifier.verify(authHeader.substring(7));
            me.ifPresent(principal -> SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())))));
        }

        String presented = request.getHeader(INTERNAL_HEADER);
        if (presented != null && internalKey.length > 0
                && MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), internalKey)) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "internal-service",
                            null,
                            List.of(new SimpleGrantedAuthority("INTERNAL"))));
        }

        chain.doFilter(request, response);
    }
}
