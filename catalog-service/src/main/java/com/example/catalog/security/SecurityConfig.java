package com.example.catalog.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Catalog access rules.
 *
 * <ul>
 *   <li>Public reads: categories, services, approved technicians, search, and a
 *       technician's slots (what customers browse).</li>
 *   <li>{@code /internal/**}: only other QuickFix services (X-Internal-Key).</li>
 *   <li>Everything else: a signed-in user. Role and ownership rules are enforced
 *       with {@code @PreAuthorize} and in the services.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiAuthenticationFilter authenticationFilter) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/internal/**").hasAuthority("INTERNAL")
                .requestMatchers(HttpMethod.GET,
                        "/api/categories", "/api/categories/**",
                        "/api/services", "/api/services/**",
                        "/api/technicians", "/api/technicians/search",
                        "/api/technicians/*", "/api/technicians/*/slots").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, ex) ->
                        SecurityErrors.write(response, request, 401,
                                "Unauthorized", "Sign in to continue"))
                .accessDeniedHandler((request, response, ex) ->
                        SecurityErrors.write(response, request, 403,
                                "Forbidden", "You are not allowed to do this")))
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
