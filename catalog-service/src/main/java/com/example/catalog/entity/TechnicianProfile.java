package com.example.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A technician's provider profile.
 *
 * <p>The technician's <em>account</em> (name, email, phone, login) lives in
 * user-service as a {@code User} with role {@code TECHNICIAN}; this profile
 * holds only the catalog/marketplace side of them and references that account
 * by {@link #userId} (a logical id, not a database foreign key).
 *
 * <p>Rating is <em>computed</em> from reviews (a later step) and cached here as
 * {@link #ratingAvg} / {@link #ratingCount}; it is never set by the technician.
 */
@Entity
@Table(name = "technician_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References the User account in user-service. One profile per user. */
    @Column(nullable = false, unique = true)
    private Long userId;

    /** Category ids this technician services (e.g. AC, washing machine). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "technician_skills",
            joinColumns = @JoinColumn(name = "technician_profile_id"))
    @Column(name = "category_id")
    @Builder.Default
    private List<Long> skills = new ArrayList<>();

    /** Pincodes the technician is willing to serve. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "technician_service_areas",
            joinColumns = @JoinColumn(name = "technician_profile_id"))
    @Column(name = "pincode")
    @Builder.Default
    private List<String> serviceAreas = new ArrayList<>();

    private Integer yearsExperience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Double ratingAvg = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;
}
