package com.example.QuickFix.controller;

import com.example.QuickFix.DTO.UserResponse;
import com.example.QuickFix.entity.User;
import com.example.QuickFix.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuickFix.DTO.UpdateUserRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getMyProfile(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        User user = userService.findById(userId);

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getProfilePicture(),
                user.getRole()
        );
    }

    @PutMapping("/me")
public UserResponse updateMyProfile(
        Authentication authentication,
        @RequestBody UpdateUserRequest request) {

    Long userId = (Long) authentication.getPrincipal();

    User user = userService.updateProfile(
            userId,
            request
    );

    return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getPhone(),
            user.getProfilePicture(),
            user.getRole()
    );
}
}
