package com.example.QuickFix.DTO;

import com.example.QuickFix.entity.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private String profilePicture;
    private Role role;
}