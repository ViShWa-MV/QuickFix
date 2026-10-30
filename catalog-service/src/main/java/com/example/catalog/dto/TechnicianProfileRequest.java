package com.example.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Payload to register or update a technician profile.
 *
 * <p>On update, {@code userId} is ignored (the profile is identified by the
 * path id). Step 7 (hardening): once catalog-service sits behind auth, derive
 * {@code userId} from the authenticated principal instead of trusting the body.
 */
@Getter
@Setter
public class TechnicianProfileRequest {

    @NotNull
    private Long userId;

    @NotEmpty
    private List<@NotNull Long> skills;

    @NotEmpty
    private List<@NotBlank String> serviceAreas;

    @NotNull
    @PositiveOrZero
    private Integer yearsExperience;
}
