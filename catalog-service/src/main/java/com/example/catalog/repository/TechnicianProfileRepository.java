package com.example.catalog.repository;

import com.example.catalog.entity.TechnicianProfile;
import com.example.catalog.entity.VerificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TechnicianProfileRepository
        extends JpaRepository<TechnicianProfile, Long> {

    boolean existsByUserId(Long userId);

    Optional<TechnicianProfile> findByUserId(Long userId);

    List<TechnicianProfile> findByVerificationStatus(VerificationStatus status, Pageable pageable);

    /**
     * Discovery search in one query: approved technicians who offer the category,
     * serve the pincode, and have an OPEN slot overlapping [from, to). Ordered by
     * rating, then experience, then number of ratings. Paged by the caller.
     */
    @Query("""
            SELECT p FROM TechnicianProfile p
            WHERE p.verificationStatus = com.example.catalog.entity.VerificationStatus.APPROVED
              AND :categoryId MEMBER OF p.skills
              AND :pincode MEMBER OF p.serviceAreas
              AND EXISTS (
                  SELECT s.id FROM AvailabilitySlot s
                  WHERE s.technicianProfileId = p.id
                    AND s.status = com.example.catalog.entity.SlotStatus.OPEN
                    AND s.startAt < :to
                    AND s.endAt > :from)
            ORDER BY p.ratingAvg DESC, p.yearsExperience DESC, p.ratingCount DESC
            """)
    List<TechnicianProfile> searchAvailable(
            @Param("categoryId") Long categoryId,
            @Param("pincode") String pincode,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    /**
     * Folds one new star rating into the cached average in a single UPDATE, so
     * concurrent reviews cannot overwrite each other. Returns rows changed (0 = no
     * such profile).
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE TechnicianProfile p
               SET p.ratingAvg = (p.ratingAvg * p.ratingCount + :rating) / (p.ratingCount + 1),
                   p.ratingCount = p.ratingCount + 1
             WHERE p.id = :id
            """)
    int addRating(@Param("id") Long id, @Param("rating") double rating);
}
