package com.example.catalog.service;

import com.example.catalog.dto.TechnicianProfileRequest;
import com.example.catalog.dto.TechnicianProfileResponse;
import com.example.catalog.dto.VerificationDecisionRequest;
import com.example.catalog.entity.TechnicianProfile;
import com.example.catalog.entity.VerificationStatus;
import com.example.catalog.exception.ResourceNotFoundException;
import com.example.catalog.repository.TechnicianProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TechnicianProfileService {

    private final TechnicianProfileRepository repository;

    public TechnicianProfileService(
            TechnicianProfileRepository repository) {
        this.repository = repository;
    }

    /** A technician registers their profile; it starts PENDING verification. */
    public TechnicianProfileResponse register(
            TechnicianProfileRequest request) {

        if (repository.existsByUserId(request.getUserId())) {
            throw new IllegalStateException(
                    "A technician profile already exists for this user");
        }

        TechnicianProfile profile = TechnicianProfile.builder()
                .userId(request.getUserId())
                .skills(request.getSkills())
                .serviceAreas(request.getServiceAreas())
                .yearsExperience(request.getYearsExperience())
                .verificationStatus(VerificationStatus.PENDING)
                .ratingAvg(0.0)
                .ratingCount(0)
                .build();

        return TechnicianProfileResponse.from(repository.save(profile));
    }

    public TechnicianProfileResponse getById(Long id) {
        return TechnicianProfileResponse.from(find(id));
    }

    public TechnicianProfileResponse getByUserId(Long userId) {
        TechnicianProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No technician profile for this user"));
        return TechnicianProfileResponse.from(profile);
    }

    /** Public discovery list: only approved technicians are visible. */
    public List<TechnicianProfileResponse> getApproved() {
        return map(repository.findByVerificationStatus(
                VerificationStatus.APPROVED));
    }

    /** Admin review queue. */
    public List<TechnicianProfileResponse> getPending() {
        return map(repository.findByVerificationStatus(
                VerificationStatus.PENDING));
    }

    /** Technician edits their own profile details (not their verification). */
    public TechnicianProfileResponse update(
            Long id,
            TechnicianProfileRequest request) {

        TechnicianProfile profile = find(id);
        profile.setSkills(request.getSkills());
        profile.setServiceAreas(request.getServiceAreas());
        profile.setYearsExperience(request.getYearsExperience());

        return TechnicianProfileResponse.from(repository.save(profile));
    }

    /** Admin approves or rejects a profile. */
    public TechnicianProfileResponse decideVerification(
            Long id,
            VerificationDecisionRequest request) {

        VerificationStatus target = request.getStatus();
        if (target != VerificationStatus.APPROVED
                && target != VerificationStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "Verification decision must be APPROVED or REJECTED");
        }

        TechnicianProfile profile = find(id);
        profile.setVerificationStatus(target);

        return TechnicianProfileResponse.from(repository.save(profile));
    }

    private TechnicianProfile find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Technician profile not found"));
    }

    private List<TechnicianProfileResponse> map(
            List<TechnicianProfile> profiles) {
        return profiles.stream()
                .map(TechnicianProfileResponse::from)
                .toList();
    }
}
