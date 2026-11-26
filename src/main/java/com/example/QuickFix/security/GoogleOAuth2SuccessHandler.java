package com.example.QuickFix.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.QuickFix.entity.User;
import com.example.QuickFix.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GoogleOAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtService jwtService;

    public GoogleOAuth2SuccessHandler(
            UserService userService,
            JwtService jwtService) {

        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        // Get authenticated Google user
        OidcUser oidcUser =
                (OidcUser) authentication.getPrincipal();

        // Get information provided by Google
        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String picture = oidcUser.getPicture();

        // Find existing QuickFix user
        // or create a new one
        User user = userService.findOrCreateGoogleUser(
                googleId,
                email,
                name,
                picture
        );


        // =====================================================
        // NEW USER
        // =====================================================

        if (!user.isOnboardingCompleted()) {

            System.out.println(
                    "New user. Onboarding required."
            );

            System.out.println(
                    "QuickFix User ID: "
                    + user.getId()
            );

            System.out.println(
                    "Email: "
                    + user.getEmail()
            );

            response.sendRedirect(
                    "http://localhost:5173/onboarding"
            );

            return;
        }


        // =====================================================
        // EXISTING USER
        // =====================================================

        String token =
                jwtService.generateToken(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name()
                );


        System.out.println(
                "Existing user."
        );

        System.out.println(
                "QuickFix User ID: "
                + user.getId()
        );

        System.out.println(
                "Email: "
                + user.getEmail()
        );

        System.out.println(
                "Role: "
                + user.getRole()
        );

        System.out.println(
                "JWT: "
                + token
        );


      // Redirect existing user to home
response.sendRedirect(
    "http://localhost:5173/home?token="
    + token
);
    }
}