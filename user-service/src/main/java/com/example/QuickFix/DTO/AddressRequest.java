package com.example.QuickFix.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A service address as the user enters it. The account is taken from the token. */
public class AddressRequest {

    @Size(max = 40)
    private String label;

    @NotBlank
    @Size(max = 200)
    private String addressLine;

    @NotBlank
    @Size(max = 80)
    private String city;

    @NotBlank
    @Size(max = 80)
    private String state;


    /** Six digits (Indian PIN code format). */
    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "must be a 6-digit pincode")
    private String pincode;

    public AddressRequest() {
    }

    public String getLabel() { return label; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPincode() { return pincode; }

    public void setLabel(String label) { this.label = label; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setPincode(String pincode) { this.pincode = pincode; }
}
