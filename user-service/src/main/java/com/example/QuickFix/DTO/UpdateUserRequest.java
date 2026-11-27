package com.example.QuickFix.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Profile details the user may change. Phone is optional; if given it must look like a phone number. */
public class UpdateUserRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,20}$", message = "must be a phone number")
    private String phone;

    public UpdateUserRequest() {
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }

    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
}
