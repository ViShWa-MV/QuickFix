package com.example.catalog.service;

import com.example.catalog.dto.RatingRequest;
import com.example.catalog.dto.TechnicianProfileRequest;
import com.example.catalog.dto.TechnicianProfileResponse;
import com.example.catalog.dto.VerificationDecisionRequest;
import com.example.catalog.entity.TechnicianProfile;
import com.example.catalog.entity.VerificationStatus;
import com.example.catalog.exception.ResourceNotFoundException;
import com.example.catalog.repository.TechnicianProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TechnicianProfileService {

    private final TechnicianProfileRepository repository;
    private final boolean autoApproveTechnicians;

    public TechnicianProfileService(
            TechnicianProfileRepository repository,
            @Value("${quickfix.auto-approve-technicians:false}") boolean autoApproveTechnicians) {
        this.repository = repository;
        this.autoApproveTechnicians = autoApproveTechnicians;
    }

    /**
     * Registers the signed-in technician's profile. It starts PENDING unless the
     * development auto-approve setting is on.
     */
    public TechnicianProfileResponse register(Long userId, TechnicianProfileRequest request) {

        if (repository.existsByUserId(userId)) {
            throw new IllegalStateException(
                    "A technician profile already exists for this user");
        }

        TechnicianProfile profile = TechnicianProfile.builder()
                .userId(userId)
                .displayName(request.getDisplayName().trim())
                .skills(request.getSkills())
                .serviceAreas(request.getServiceAreas())
                .yearsExperience(request.getYearsExperience())
                .verificationStatus(autoApproveTechnicians
                        ? VerificationStatus.APPROVED
                        : VerificationStatus.PENDING)
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
    public List<TechnicianProfileResponse> getApproved(Pageable pageable) {
        return map(repository.findByVerificationStatus(VerificationStatus.APPROVED, pageable));
    }

    /** Admin review queue. */
    public List<TechnicianProfileResponse> getPending(Pageable pageable) {
        return map(repository.findByVerificationStatus(VerificationStatus.PENDING, pageable));
    }

    /** Technician edits their own profile details (not their verification). */
    public TechnicianProfileResponse update(Long id, TechnicianProfileRequest request) {

        TechnicianProfile profile = find(id);
        profile.setDisplayName(request.getDisplayName().trim());
        profile.setSkills(request.getSkills());
        profile.setServiceAreas(request.getServiceAreas());
        profile.setYearsExperience(request.getYearsExperience());

        return TechnicianProfileResponse.from(repository.save(profile));
    }

    /** Admin approves or rejects a profile. */
    public TechnicianProfileResponse decideVerification(Long id, VerificationDecisionRequest request) {

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

    /**
     * Discovery search: approved technicians skilled in {@code categoryId}, serving
     * {@code pincode}, with an OPEN slot overlapping [from, to). Filtering, ordering,
     * and paging all happen in the database.
     */
    public List<TechnicianProfileResponse> search(
            Long categoryId,
            String pincode,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("'to' must be after 'from'");
        }
        return map(repository.searchAvailable(categoryId, pincode, from, to, pageable));
    }

    /**
     * Called by booking-service after a customer rates a completed job. Folds the
     * rating into the cached average atomically.
     */
    @Transactional
    public void addRating(Long id, RatingRequest request) {
        int updated = repository.addRating(id, request.getRating().doubleValue());
        if (updated == 0) {
            throw new ResourceNotFoundException("Technician profile not found");
        }
    }

    private TechnicianProfile find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician profile not found"));
    }

    private List<TechnicianProfileResponse> map(List<TechnicianProfile> profiles) {
        return profiles.stream()
                .map(TechnicianProfileResponse::from)
                .toList();
    }
}
