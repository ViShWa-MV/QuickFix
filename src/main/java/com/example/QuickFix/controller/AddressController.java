package com.example.QuickFix.controller;

import com.example.QuickFix.DTO.AddressRequest;
import com.example.QuickFix.DTO.AddressResponse;
import com.example.QuickFix.service.AddressService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public AddressResponse createAddress(
            Authentication authentication,
            @RequestBody AddressRequest request) {

        Long userId = (Long) authentication.getPrincipal();

        return addressService.createAddress(
                userId,
                request
        );
    }

    @GetMapping
    public List<AddressResponse> getMyAddresses(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return addressService.getMyAddresses(userId);
    }

    @GetMapping("/{addressId}")
    public AddressResponse getMyAddress(
            Authentication authentication,
            @PathVariable Long addressId) {

        Long userId = (Long) authentication.getPrincipal();

        return addressService.getMyAddress(
                addressId,
                userId
        );
    }

    @PutMapping("/{addressId}")
    public AddressResponse updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @RequestBody AddressRequest request) {

        Long userId = (Long) authentication.getPrincipal();

        return addressService.updateAddress(
                addressId,
                userId,
                request
        );
    }

    @DeleteMapping("/{addressId}")
    public String deleteAddress(
            Authentication authentication,
            @PathVariable Long addressId) {

        Long userId = (Long) authentication.getPrincipal();

        addressService.deleteAddress(
                addressId,
                userId
        );

        return "Address deleted successfully";
    }
}