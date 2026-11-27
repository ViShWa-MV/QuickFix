package com.example.QuickFix.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String label;

    @Column(nullable = false)
    private String addressLine;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    @Column(nullable = false)
    private Long userId;

    /** The street as chosen from the map, e.g. "MG Road". */
    @Column(name = "street_name", length = 160)
    private String streetName;

    /** Neighbourhood or locality from the map, if known. */
    @Column(length = 160)
    private String locality;

    private Double latitude;

    private Double longitude;

    /** OpenStreetMap id of the place the street was matched to. */
    @Column(name = "osm_place_id")
    private Long osmPlaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
}