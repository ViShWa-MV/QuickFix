package com.example.QuickFix.config;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.QuickFix.security.GoogleOAuth2SuccessHandler;
import com.example.QuickFix.security.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final GoogleOAuth2SuccessHandler successHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            GoogleOAuth2SuccessHandler successHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.successHandler = successHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .cors(cors -> {})

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/",
                    "/api/auth/success",
                    "/oauth2/**",
                    "/login/**"
                ).permitAll()

                .anyRequest().authenticated()
            )

            .exceptionHandling(exception -> exception

                .authenticationEntryPoint(
                    (request, response, authException) -> {

                        response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Authentication required"
                        );
                    }
                )

                .accessDeniedHandler(
                    (request, response, accessDeniedException) -> {

                        response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Access denied"
                        );
                    }
                )
            )

            .oauth2Login(oauth2 -> oauth2
                .successHandler(successHandler)
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}