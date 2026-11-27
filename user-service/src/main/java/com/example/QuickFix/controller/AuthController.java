package com.example.QuickFix.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.QuickFix.security.OneTimeCodeStore;

/**
 * Public endpoint that trades a one-time sign-in code for the JWT.
 * The code is single-use and expires after 60 seconds.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OneTimeCodeStore codeStore;

    public AuthController(OneTimeCodeStore codeStore) {
        this.codeStore = codeStore;
    }

    @PostMapping("/exchange")
    public Map<String, String> exchange(@RequestBody Map<String, String> body) {
        return codeStore.redeem(body.get("code"))
                .map(token -> Map.of("token", token))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Sign-in code is invalid or expired"));
    }
}
