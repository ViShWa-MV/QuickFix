package com.example.QuickFix.DTO;

public class AddressResponse {

    private Long id;
    private String label;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;

    public AddressResponse(
            Long id,
            String label,
            String addressLine,
            String city,
            String state,
            String pincode) {

        this.id = id;
        this.label = label;
        this.addressLine = addressLine;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }
}