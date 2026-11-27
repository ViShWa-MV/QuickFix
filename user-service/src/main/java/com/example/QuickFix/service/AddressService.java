package com.example.QuickFix.service;

import com.example.QuickFix.DTO.AddressRequest;
import com.example.QuickFix.DTO.AddressResponse;
import com.example.QuickFix.entity.Address;
import com.example.QuickFix.entity.VerificationStatus;
import com.example.QuickFix.exception.ResourceNotFoundException;
import com.example.QuickFix.repository.AddressRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepo addressRepository;
    private final AddressVerifier verifier;

    public AddressService(AddressRepo addressRepository, AddressVerifier verifier) {
        this.addressRepository = addressRepository;
        this.verifier = verifier;
    }

    /**
     * Saves the user's address. Each user has exactly one; after that it can only
     * be updated. The address is verified before it is stored, so an unverified
     * address is never saved.
     */
    public AddressResponse createAddress(Long userId, AddressRequest request) {

        if (!addressRepository.findByUserId(userId).isEmpty()) {
            throw new IllegalStateException("An address already exists. Edit it instead.");
        }

        Address address = new Address();
        address.setUserId(userId);
        apply(address, request);

        return AddressResponse.from(addressRepository.save(address));
    }

    public List<AddressResponse> getMyAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    public AddressResponse getMyAddress(Long addressId, Long userId) {
        return AddressResponse.from(find(addressId, userId));
    }

    /** Re-verifies the changed address before it replaces the old one. */
    public AddressResponse updateAddress(Long addressId, Long userId, AddressRequest request) {
        Address address = find(addressId, userId);
        apply(address, request);
        return AddressResponse.from(addressRepository.save(address));
    }

    public void deleteAddress(Long addressId, Long userId) {
        addressRepository.delete(find(addressId, userId));
    }

    /**
     * Checks the pincode, state, and city against the official list, then stores
     * the official state and district (not what was typed). No street or map point
     * is stored.
     */
    private void apply(Address address, AddressRequest request) {
        AddressVerifier.VerifiedPlace place = verifier.verify(
                request.getPincode(), request.getState(), request.getCity());

        address.setLabel(request.getLabel());
        address.setAddressLine(request.getAddressLine());
        address.setCity(place.district());
        address.setState(place.state());
        address.setPincode(request.getPincode());
        address.setStreetName(null);
        address.setLocality(null);
        address.setLatitude(null);
        address.setLongitude(null);
        address.setOsmPlaceId(null);
        address.setVerificationStatus(VerificationStatus.VERIFIED);
    }

    private Address find(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }
}
