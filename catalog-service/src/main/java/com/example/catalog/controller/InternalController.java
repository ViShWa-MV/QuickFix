package com.example.catalog.controller;

import com.example.catalog.dto.RatingRequest;
import com.example.catalog.dto.SlotResponse;
import com.example.catalog.dto.TechnicianProfileResponse;
import com.example.catalog.exception.ResourceNotFoundException;
import com.example.catalog.repository.TechnicianProfileRepository;
import com.example.catalog.service.AvailabilityService;
import com.example.catalog.service.TechnicianProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Service-to-service endpoints, called by booking-service only. Every path here
 * requires the internal key (see SecurityConfig), so none of them is reachable
 * from a browser.
 */
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final AvailabilityService availability;
    private final TechnicianProfileService profiles;
    private final TechnicianProfileRepository profileRepository;

    public InternalController(
            AvailabilityService availability,
            TechnicianProfileService profiles,
            TechnicianProfileRepository profileRepository) {
        this.availability = availability;
        this.profiles = profiles;
        this.profileRepository = profileRepository;
    }

    @PostMapping("/slots/{slotId}/hold")
    public SlotResponse hold(
            @PathVariable Long slotId,
            @RequestParam Long profileId) {
        return availability.hold(slotId, profileId);
    }

    @PostMapping("/slots/{slotId}/book")
    public SlotResponse book(@PathVariable Long slotId) {
        return availability.book(slotId);
    }

    @PostMapping("/slots/{slotId}/release")
    public SlotResponse release(@PathVariable Long slotId) {
        return availability.release(slotId);
    }

    @PostMapping("/technicians/{id}/ratings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addRating(@PathVariable Long id, @Valid @RequestBody RatingRequest request) {
        profiles.addRating(id, request);
    }

    /** A profile by id, including its owner account and approval status. */
    @GetMapping("/technicians/{id}")
    public TechnicianProfileResponse getProfile(@PathVariable Long id) {
        return profiles.getById(id);
    }

    /** The profile id for a user account, or 404 if that user is not a technician. */
    @GetMapping("/technicians/by-user/{userId}")
    public TechnicianProfileResponse getProfileByUser(@PathVariable Long userId) {
        return profileRepository.findByUserId(userId)
                .map(TechnicianProfileResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No technician profile for this user"));
    }
}
