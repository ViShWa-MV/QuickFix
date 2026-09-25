package com.example.QuickFix.service;

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
                    user.setRole(Role.CUSTOMER);

                    return userRepository.save(user);
                });
    }
}