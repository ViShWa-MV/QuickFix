package com.example.booking.client;

import com.example.booking.exception.AddressUnavailableException;
import com.example.booking.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads a customer's saved address from user-service over the internal API.
 * The internal key is sent on every call; the address owner is checked by the
 * caller, not here.
 */
@Component
public class UserServiceClient {

    /** The address as user-service returns it on the internal API. */
    public record AddressSnapshot(
            Long id,
            Long ownerUserId,
            String label,
            String addressLine,
            String locality,
            String streetName,
            String city,
            String state,
            String pincode,
            Double latitude,
            Double longitude,
            String verificationStatus) {
    }

    private final RestClient restClient;

    public UserServiceClient(
            @Value("${user.base-url:http://localhost:8081}") String baseUrl,
            @Value("${quickfix.internal-key:}") String internalKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Key", internalKey)
                .build();
    }

    public AddressSnapshot getAddress(Long addressId) {
        try {
            AddressSnapshot snapshot = restClient.get()
                    .uri("/internal/addresses/{id}", addressId)
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Address not found");
                    })
                    .body(AddressSnapshot.class);
            if (snapshot == null) {
                throw new AddressUnavailableException("empty address response", null);
            }
            return snapshot;
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new AddressUnavailableException("The address service is unavailable. Try again shortly.", ex);
        }
    }
}
