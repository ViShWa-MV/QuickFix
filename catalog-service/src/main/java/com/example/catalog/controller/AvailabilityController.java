package com.example.catalog.controller;

import com.example.catalog.dto.SlotGenerationRequest;
import com.example.catalog.dto.SlotRequest;
import com.example.catalog.dto.SlotResponse;
import com.example.catalog.security.AuthPrincipal;
import com.example.catalog.service.AvailabilityService;
import com.example.catalog.service.ProfileAccess;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * A technician's availability slots. Anyone may read them (customers browse);
 * only the technician who owns the profile, or an admin, may change them.
 */
@RestController
@RequestMapping("/api/technicians/{profileId}/slots")
public class AvailabilityController {

    private final AvailabilityService service;
    private final ProfileAccess profileAccess;

    public AvailabilityController(AvailabilityService service, ProfileAccess profileAccess) {
        this.service = service;
        this.profileAccess = profileAccess;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public SlotResponse create(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long profileId,
            @Valid @RequestBody SlotRequest request) {
        profileAccess.requireOwnerOrAdmin(profileId, me);
        return service.createSlot(profileId, request);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public List<SlotResponse> generate(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long profileId,
            @Valid @RequestBody SlotGenerationRequest request) {
        profileAccess.requireOwnerOrAdmin(profileId, me);
        return service.generate(profileId, request);
    }

    @GetMapping
    public List<SlotResponse> list(@PathVariable Long profileId) {
        return service.listSlots(profileId);
    }

    @DeleteMapping("/{slotId}")
    @PreAuthorize("isAuthenticated()")
    public String delete(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long profileId,
            @PathVariable Long slotId) {
        profileAccess.requireOwnerOrAdmin(profileId, me);
        service.deleteSlot(profileId, slotId);
        return "Slot removed";
    }
}
