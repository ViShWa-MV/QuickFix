package com.example.QuickFix.service;

import com.example.QuickFix.DTO.UpdateUserRequest;
import com.example.QuickFix.entity.Role;
import com.example.QuickFix.entity.User;
import com.example.QuickFix.repository.UserRepo;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepo userRepository;

    public UserService(UserRepo userRepository) {
        this.userRepository = userRepository;
    }

    public User findOrCreateGoogleUser(
            String googleId,
            String email,
            String name,
            String picture) {

        return userRepository.findByGoogleId(googleId)
                .orElseGet(() -> {

                    User user = new User();

                    user.setGoogleId(googleId);
                    user.setEmail(email);
                    user.setName(name);
                    user.setProfilePicture(picture);

                    // New user has not selected a role yet
                    user.setRole(null);
                    user.setOnboardingCompleted(false);

                    return userRepository.save(user);
                });
    }

    public User findByGoogleId(String googleId) {

        return userRepository.findByGoogleId(googleId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    public User completeOnboarding(
            Long userId,
            Role role) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.isOnboardingCompleted()) {
            throw new RuntimeException(
                    "Onboarding already completed");
        }

        // Only CUSTOMER or TECHNICIAN are allowed
        if (role != Role.CUSTOMER &&
            role != Role.TECHNICIAN) {

            throw new RuntimeException(
                    "Invalid role. Choose CUSTOMER or TECHNICIAN");
        }

        user.setRole(role);
        user.setOnboardingCompleted(true);

        return userRepository.save(user);
    }

    public User findById(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    public User updateProfile(
            Long userId,
            UpdateUserRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setName(request.getName());
        user.setPhone(request.getPhone());

        return userRepository.save(user);
    }
}