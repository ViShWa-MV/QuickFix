package com.example.booking.client;

import com.example.booking.exception.CatalogUnavailableException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.exception.SlotUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

/**
 * Talks to catalog-service, which owns slot state, service pricing, and
 * technician profiles.
 *
 * <p>Slot and rating calls go to catalog's {@code /internal} endpoints and carry
 * the shared internal key. Slot transitions are compare-and-set on catalog's
 * side: a {@code 409} means the slot was not in the expected state, surfaced as
 * {@link SlotUnavailableException}. Transport or 5xx failures become
 * {@link CatalogUnavailableException}.
 */
@Component
public class CatalogClient {

    private static final Logger log =
            LoggerFactory.getLogger(CatalogClient.class);

    /** The parts of a technician profile booking needs. */
    public record TechnicianView(Long id, Long userId, String verificationStatus) {
    }

    private record ServiceView(Long id, Double price) {
    }

    private final RestClient restClient;

    public CatalogClient(
            @Value("${catalog.base-url:http://localhost:8082}") String baseUrl,
            @Value("${quickfix.internal-key:}") String internalKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Key", internalKey)
                .build();
    }

    /** OPEN -> HELD, for a slot the given technician profile publishes. */
    public void holdSlot(Long slotId, Long profileId) {
        try {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/slots/{id}/hold")
                            .queryParam("profileId", profileId)
                            .build(slotId))
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Slot not found: " + slotId);
                    })
                    .onStatus(s -> s.value() == 409, (req, res) -> {
                        throw new SlotUnavailableException("Slot " + slotId + " is not available");
                    })
                    .toBodilessEntity();
        } catch (ResourceNotFoundException | SlotUnavailableException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new CatalogUnavailableException("catalog-service unavailable during slot hold", ex);
        }
    }

    /** HELD -> BOOKED. Throws if the slot is no longer held. */
    public void bookSlot(Long slotId) {
        transition(slotId, "book");
    }

    /**
     * HELD/BOOKED -> OPEN, best-effort. A failed release is logged, not thrown:
     * the user-facing action must still succeed, and the catalog sweep is the
     * safety net.
     */
    public void releaseSlotQuietly(Long slotId) {
        try {
            restClient.post()
                    .uri("/internal/slots/{id}/release", slotId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Failed to release slot {} (will be swept): {}",
                    slotId, ex.getMessage());
        }
    }

    private void transition(Long slotId, String action) {
        try {
            restClient.post()
                    .uri("/internal/slots/{id}/{action}", slotId, action)
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException(
                                "Slot not found: " + slotId);
                    })
                    .onStatus(s -> s.value() == 409, (req, res) -> {
                        throw new SlotUnavailableException(
                                "Slot " + slotId + " is not available to " + action);
                    })
                    .toBodilessEntity();
        } catch (ResourceNotFoundException | SlotUnavailableException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new CatalogUnavailableException(
                    "catalog-service unavailable during slot " + action, ex);
        }
    }

    /** A technician profile by id, with its owner account and approval status. */
    public TechnicianView getTechnician(Long profileId) {
        try {
            TechnicianView view = restClient.get()
                    .uri("/internal/technicians/{id}", profileId)
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException(
                                "Technician not found: " + profileId);
                    })
                    .body(TechnicianView.class);
            if (view == null) {
                throw new CatalogUnavailableException("empty technician response", null);
            }
            return view;
        } catch (ResourceNotFoundException | CatalogUnavailableException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new CatalogUnavailableException(
                    "catalog-service unavailable during technician lookup", ex);
        }
    }

    /** The technician profile id for a user account, if that user is a technician. */
    public Optional<Long> technicianProfileIdFor(Long userId) {
        try {
            TechnicianView view = restClient.get()
                    .uri("/internal/technicians/by-user/{userId}", userId)
                    .retrieve()
                    .body(TechnicianView.class);
            return Optional.ofNullable(view).map(TechnicianView::id);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw new CatalogUnavailableException(
                    "catalog-service unavailable during profile lookup", ex);
        }
    }

    /** Snapshot a service's base price at booking time. */
    public double getServicePrice(Long serviceId) {
        try {
            ServiceView view = restClient.get()
                    .uri("/api/services/{id}", serviceId)
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException(
                                "Service not found: " + serviceId);
                    })
                    .body(ServiceView.class);

            return (view != null && view.price() != null) ? view.price() : 0.0;
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new CatalogUnavailableException(
                    "catalog-service unavailable during price lookup", ex);
        }
    }

    /**
     * Folds one customer star rating into the technician's cached average.
     * Failure is NOT swallowed: the caller's transaction must roll back the review
     * so the rating and the review cannot disagree.
     */
    public void addRating(Long profileId, int rating) {
        try {
            restClient.post()
                    .uri("/internal/technicians/{id}/ratings", profileId)
                    .body(Map.of("rating", rating))
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException(
                                "Technician profile not found: " + profileId);
                    })
                    .toBodilessEntity();
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new CatalogUnavailableException(
                    "catalog-service unavailable while recording rating", ex);
        }
    }
}
