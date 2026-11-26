package com.example.catalog.dto;

import com.example.catalog.entity.TechnicianProfile;
import com.example.catalog.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Technician profile as returned to clients. Keeps the API decoupled from the
 * JPA entity so persistence changes don't leak into the contract.
 */
@Getter
@AllArgsConstructor
public class TechnicianProfileResponse {

    private Long id;
    private Long userId;
    private List<Long> skills;
    private List<String> serviceAreas;
    private Integer yearsExperience;
    private VerificationStatus verificationStatus;
    private Double ratingAvg;
    private Integer ratingCount;

    public static TechnicianProfileResponse from(TechnicianProfile p) {
        return new TechnicianProfileResponse(
                p.getId(),
                p.getUserId(),
                p.getSkills(),
                p.getServiceAreas(),
                p.getYearsExperience(),
                p.getVerificationStatus(),
                p.getRatingAvg(),
                p.getRatingCount());
    }
}
