package com.example.QuickFix.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuickFix.entity.User;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByGoogleId(String googleId);
}
