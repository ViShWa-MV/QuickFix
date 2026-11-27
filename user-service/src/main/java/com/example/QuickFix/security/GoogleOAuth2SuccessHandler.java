package com.example.QuickFix.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.QuickFix.entity.User;
import com.example.QuickFix.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * After Google sign-in: new users go to onboarding; existing users get a
 * one-time code that the SPA exchanges for a JWT (see AuthController).
 *
 * <p>No token, email, or other secret is logged or placed in a URL.
 */
@Component
public class GoogleOAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GoogleOAuth2SuccessHandler.class);

    private final UserService userService;
    private final OneTimeCodeStore codeStore;
    private final JwtService jwtService;
    private final String frontendUrl;

    public GoogleOAuth2SuccessHandler(
            UserService userService,
            OneTimeCodeStore codeStore,
            JwtService jwtService,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {

        this.userService = userService;
        this.codeStore = codeStore;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OidcUser oidcUser =
                (OidcUser) authentication.getPrincipal();

        User user = userService.findOrCreateGoogleUser(
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                oidcUser.getFullName(),
                oidcUser.getPicture()
        );

        if (!user.isOnboardingCompleted()) {
            log.info("New user {} needs onboarding", user.getId());
            response.sendRedirect(frontendUrl + "/onboarding");
            return;
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String code = codeStore.issue(token);

        log.info("Signed in user {} ({})", user.getId(), user.getRole());

        response.sendRedirect(
                UriComponentsBuilder.fromUriString(frontendUrl)
                        .path("/home")
                        .queryParam("code", code)
                        .build()
                        .toUriString());
    }
}
