package com.example.QuickFix.controller;

import com.example.QuickFix.DTO.AuthResponse;
import com.example.QuickFix.DTO.OnboardingRequest;
import com.example.QuickFix.entity.User;
import com.example.QuickFix.security.JwtService;
import com.example.QuickFix.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class OnboardingController {

    private final UserService userService;
    private final JwtService jwtService;

    public OnboardingController(
            UserService userService,
            JwtService jwtService) {

        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/onboarding")
    public AuthResponse completeOnboarding(
            Authentication authentication,
            @RequestBody OnboardingRequest request) {

        // Get Google user from OAuth2 session
        OidcUser oidcUser =
                (OidcUser) authentication.getPrincipal();

        // Google's unique user ID
        String googleId = oidcUser.getSubject();

        // Find corresponding QuickFix user
        User existingUser =
                userService.findByGoogleId(googleId);

        // Complete onboarding
        User user = userService.completeOnboarding(
                existingUser.getId(),
                request.getRole()
        );

        // Generate QuickFix JWT after role selection
        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        // Return structured authentication response
        return new AuthResponse(
                "Onboarding completed",
                token,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}