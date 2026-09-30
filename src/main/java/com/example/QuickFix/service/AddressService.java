package com.example.QuickFix.service;

import com.example.QuickFix.DTO.AddressRequest;
import com.example.QuickFix.DTO.AddressResponse;
import com.example.QuickFix.entity.Address;
import com.example.QuickFix.repository.AddressRepo;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepo addressRepository;

    public AddressService(AddressRepo addressRepository) {
        this.addressRepository = addressRepository;
    }

    // Create a new address for the logged-in user
    public AddressResponse createAddress(
            Long userId,
            AddressRequest request) {

        Address address = new Address();

        address.setUserId(userId);
        address.setLabel(request.getLabel());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());

        Address savedAddress =
                addressRepository.save(address);

        return convertToResponse(savedAddress);
    }

    // Get all addresses belonging to the logged-in user
    public List<AddressResponse> getMyAddresses(Long userId) {

        return addressRepository.findByUserId(userId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get one address belonging to the logged-in user
    public AddressResponse getMyAddress(
            Long addressId,
            Long userId) {

        Address address =
                addressRepository
                        .findByIdAndUserId(addressId, userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Address not found"));

        return convertToResponse(address);
    }

    // Update an address belonging to the logged-in user
    public AddressResponse updateAddress(
            Long addressId,
            Long userId,
            AddressRequest request) {

        Address address =
                addressRepository
                        .findByIdAndUserId(addressId, userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Address not found"));

        address.setLabel(request.getLabel());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());

        Address updatedAddress =
                addressRepository.save(address);

        return convertToResponse(updatedAddress);
    }

    // Delete an address belonging to the logged-in user
    public void deleteAddress(
            Long addressId,
            Long userId) {

        Address address =
                addressRepository
                        .findByIdAndUserId(addressId, userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Address not found"));

        addressRepository.delete(address);
    }

    // Convert Entity → Response DTO
    private AddressResponse convertToResponse(
            Address address) {

        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getAddressLine(),
                address.getCity(),
                address.getState(),
                address.getPincode()
        );
    }
}