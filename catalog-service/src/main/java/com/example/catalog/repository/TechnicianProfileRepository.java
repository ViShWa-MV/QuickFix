package com.example.catalog.repository;

import com.example.catalog.entity.TechnicianProfile;
import com.example.catalog.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TechnicianProfileRepository
        extends JpaRepository<TechnicianProfile, Long> {

    boolean existsByUserId(Long userId);

    Optional<TechnicianProfile> findByUserId(Long userId);

    List<TechnicianProfile> findByVerificationStatus(VerificationStatus status);
}
