package com.example.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Payload to register or update a technician profile. The account comes from
 * the token, not from this body.
 */
@Getter
@Setter
public class TechnicianProfileRequest {

    @NotBlank
    @Size(max = 80)
    private String displayName;

    @NotEmpty
    private List<@NotNull Long> skills;

    @NotEmpty
    private List<@NotBlank String> serviceAreas;

    @NotNull
    @PositiveOrZero
    private Integer yearsExperience;
}
