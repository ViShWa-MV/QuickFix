package com.example.catalog.controller;

import com.example.catalog.dto.TechnicianProfileRequest;
import com.example.catalog.dto.TechnicianProfileResponse;
import com.example.catalog.dto.VerificationDecisionRequest;
import com.example.catalog.exception.ForbiddenException;
import com.example.catalog.security.AuthPrincipal;
import com.example.catalog.service.ProfileAccess;
import com.example.catalog.service.TechnicianProfileService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Technician provider profiles and admin verification.
 *
 * <p>Who may do what:
 * <ul>
 *   <li>Anyone: approved list, search, one profile.</li>
 *   <li>Technician: register (for their own account only), edit own profile.</li>
 *   <li>Admin: review queue and approve/reject.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/technicians")
public class TechnicianProfileController {

    private final TechnicianProfileService service;
    private final ProfileAccess profileAccess;

    public TechnicianProfileController(
            TechnicianProfileService service,
            ProfileAccess profileAccess) {
        this.service = service;
        this.profileAccess = profileAccess;
    }

    /** Register the signed-in technician's profile. The account comes from the token. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TECHNICIAN')")
    public TechnicianProfileResponse register(
            @AuthenticationPrincipal AuthPrincipal me,
            @Valid @RequestBody TechnicianProfileRequest request) {
        return service.register(me.userId(), request);
    }

    /** Public discovery list: approved technicians only. */
    @GetMapping
    public List<TechnicianProfileResponse> getApproved(@PageableDefault(size = 50) Pageable pageable) {
        return service.getApproved(pageable);
    }

    /** Discovery search: approved, skilled in the category, serving the pincode, free in the window. */
    @GetMapping("/search")
    public List<TechnicianProfileResponse> search(
            @RequestParam Long category,
            @RequestParam String pincode,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.search(category, pincode, from, to, pageable);
    }

    /** Admin review queue. */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TechnicianProfileResponse> getPending(@PageableDefault(size = 50) Pageable pageable) {
        return service.getPending(pageable);
    }

    @GetMapping("/{id}")
    public TechnicianProfileResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /** A user's own profile (or any, for an admin). */
    @GetMapping("/by-user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public TechnicianProfileResponse getByUserId(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long userId) {
        if (!me.isAdmin() && !me.userId().equals(userId)) {
            throw new ForbiddenException("You can only view your own technician profile");
        }
        return service.getByUserId(userId);
    }

    /** Edit a profile's details. Only its owner or an admin. */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TechnicianProfileResponse update(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long id,
            @Valid @RequestBody TechnicianProfileRequest request) {
        profileAccess.requireOwnerOrAdmin(id, me);
        return service.update(id, request);
    }

    /** Admin approves or rejects a profile. */
    @PatchMapping("/{id}/verification")
    @PreAuthorize("hasRole('ADMIN')")
    public TechnicianProfileResponse decideVerification(
            @PathVariable Long id,
            @Valid @RequestBody VerificationDecisionRequest request) {
        return service.decideVerification(id, request);
    }
}
