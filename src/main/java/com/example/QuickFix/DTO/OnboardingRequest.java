package com.example.QuickFix.DTO;

import com.example.QuickFix.entity.Role;

public class OnboardingRequest {

    private Role role;

    public OnboardingRequest() {
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}