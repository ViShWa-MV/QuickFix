package com.example.QuickFix.security;

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

/**
 * Grants the INTERNAL authority to a request that presents the shared service key.
 * Used only for /internal/**, which other QuickFix services call.
 */
@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Internal-Key";

    private final byte[] key;

    public InternalKeyFilter(@Value("${quickfix.internal-key:}") String key) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        if (presented != null && key.length > 0
                && MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), key)) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "internal-service", null, List.of(new SimpleGrantedAuthority("INTERNAL"))));
        }
        chain.doFilter(request, response);
    }
}
