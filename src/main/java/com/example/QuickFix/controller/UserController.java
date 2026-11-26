package com.example.QuickFix.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @GetMapping("/success")
    public String success() {
        return "Google Login Successful";
    }

    @GetMapping("/me")
    public String me(Authentication authentication) {

        return "Authenticated user: "
                + authentication.getPrincipal()
                + " | Authorities: "
                + authentication.getAuthorities();
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/customer-test")
    public String customerTest() {

        return "Customer endpoint accessed successfully";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin-test")
    public String adminTest() {

        return "Admin endpoint accessed successfully";
    }
}