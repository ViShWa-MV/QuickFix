package com.example.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** The code the technician enters to pass an OTP gate. */
@Getter
@Setter
public class OtpSubmitRequest {

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "must be a 6-digit code")
    private String otp;
}
