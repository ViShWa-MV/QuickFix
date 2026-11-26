package com.example.catalog.controller;

import com.example.catalog.dto.TechnicianProfileRequest;
import com.example.catalog.dto.TechnicianProfileResponse;
import com.example.catalog.dto.VerificationDecisionRequest;
import com.example.catalog.service.TechnicianProfileService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Technician provider profiles and admin verification.
 *
 * <p>Step 7 (hardening): the review queue and verification-decision endpoints
 * must be restricted to ADMIN (e.g. {@code @PreAuthorize("hasRole('ADMIN')")})
 * once catalog-service sits behind authentication. They are currently open.
 */
@RestController
@RequestMapping("/api/technicians")
public class TechnicianProfileController {

    private final TechnicianProfileService service;

    public TechnicianProfileController(
            TechnicianProfileService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TechnicianProfileResponse register(
            @Valid @RequestBody TechnicianProfileRequest request) {
        return service.register(request);
    }

    /** Public discovery list — approved technicians only. */
    @GetMapping
    public List<TechnicianProfileResponse> getApproved() {
        return service.getApproved();
    }

    /**
     * Discovery search: approved technicians skilled in {@code category},
     * serving {@code pincode}, with a free slot overlapping [from, to).
     */
    @GetMapping("/search")
    public List<TechnicianProfileResponse> search(
            @RequestParam Long category,
            @RequestParam String pincode,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {
        return service.search(category, pincode, from, to);
    }

    /** Admin review queue — profiles awaiting verification. */
    @GetMapping("/pending")
    public List<TechnicianProfileResponse> getPending() {
        return service.getPending();
    }

    @GetMapping("/{id}")
    public TechnicianProfileResponse getById(
            @PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/by-user/{userId}")
    public TechnicianProfileResponse getByUserId(
            @PathVariable Long userId) {
        return service.getByUserId(userId);
    }

    @PutMapping("/{id}")
    public TechnicianProfileResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TechnicianProfileRequest request) {
        return service.update(id, request);
    }

    /** Admin approves or rejects a profile. */
    @PatchMapping("/{id}/verification")
    public TechnicianProfileResponse decideVerification(
            @PathVariable Long id,
            @Valid @RequestBody VerificationDecisionRequest request) {
        return service.decideVerification(id, request);
    }
}
