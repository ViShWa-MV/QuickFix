package com.example.QuickFix.DTO;

import com.example.QuickFix.entity.Role;

public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private String profilePicture;
    private Role role;

    public UserResponse(
            Long id,
            String email,
            String name,
            String phone,
            String profilePicture,
            Role role) {

        this.id = id;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.profilePicture = profilePicture;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public Role getRole() {
        return role;
    }
}