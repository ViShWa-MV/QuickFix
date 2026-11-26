package com.example.catalog.repository;

import com.example.catalog.entity.TechnicianProfile;
import com.example.catalog.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TechnicianProfileRepository
        extends JpaRepository<TechnicianProfile, Long> {

    boolean existsByUserId(Long userId);

    Optional<TechnicianProfile> findByUserId(Long userId);

    List<TechnicianProfile> findByVerificationStatus(VerificationStatus status);

    /**
     * Approved technicians who have the given skill (category id) and serve the
     * given pincode. Availability within a time window is filtered separately.
     */
    @Query("""
            SELECT p FROM TechnicianProfile p
            WHERE p.verificationStatus = com.example.catalog.entity.VerificationStatus.APPROVED
              AND :categoryId MEMBER OF p.skills
              AND :pincode MEMBER OF p.serviceAreas
            """)
    List<TechnicianProfile> findApprovedByCategoryAndArea(
            @Param("categoryId") Long categoryId,
            @Param("pincode") String pincode);
}
