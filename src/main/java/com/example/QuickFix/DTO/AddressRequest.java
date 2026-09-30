package com.example.QuickFix.DTO;

public class AddressRequest {

    private String label;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;

    public AddressRequest() {
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

    public void setLabel(String label) {
        this.label = label;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }
}