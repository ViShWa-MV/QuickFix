package com.example.catalog.service;

import com.example.catalog.entity.TechnicianProfile;
import com.example.catalog.exception.ForbiddenException;
import com.example.catalog.exception.ResourceNotFoundException;
import com.example.catalog.repository.TechnicianProfileRepository;
import com.example.catalog.security.AuthPrincipal;
import org.springframework.stereotype.Component;

/**
 * Ownership rule for technician data: the profile's own account or an admin.
 * Controllers call this before changing a profile or its slots.
 */
@Component
public class ProfileAccess {

    private final TechnicianProfileRepository profiles;

    public ProfileAccess(TechnicianProfileRepository profiles) {
        this.profiles = profiles;
    }

    public void requireOwnerOrAdmin(Long profileId, AuthPrincipal me) {
        TechnicianProfile profile = profiles.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Technician profile not found"));
        if (!me.isAdmin() && !profile.getUserId().equals(me.userId())) {
            throw new ForbiddenException("This technician profile belongs to someone else");
        }
    }
}
