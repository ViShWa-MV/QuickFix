package com.example.QuickFix.DTO;

public class AuthResponse {

    private String message;
    private String token;
    private Long userId;
    private String email;
    private String role;

    public AuthResponse() {
    }

    public AuthResponse(
            String message,
            String token,
            Long userId,
            String email,
            String role) {

        this.message = message;
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}