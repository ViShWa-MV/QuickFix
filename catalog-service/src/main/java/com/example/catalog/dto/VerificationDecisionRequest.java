package com.example.catalog.dto;

import com.example.catalog.entity.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * An admin's verification decision on a pending technician profile.
 * {@code status} must be {@link VerificationStatus#APPROVED} or
 * {@link VerificationStatus#REJECTED}.
 */
@Getter
@Setter
public class VerificationDecisionRequest {

    @NotNull
    private VerificationStatus status;

    /** Optional note, e.g. a reason for rejection. */
    private String note;
}
