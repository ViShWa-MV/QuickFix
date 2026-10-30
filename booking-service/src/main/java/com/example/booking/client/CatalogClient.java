package com.example.booking.client;

import com.example.booking.exception.CatalogUnavailableException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.exception.SlotUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Talks to catalog-service, which owns slot state and service pricing.
 *
 * <p>Slot transitions are compare-and-set on catalog's side:
 * a {@code 409} means the slot was not in the expected state (lost the race /
 * already taken / expired), surfaced here as {@link SlotUnavailableException};
 * any transport/5xx problem becomes {@link CatalogUnavailableException} so the
 * caller can decide whether the overall operation can proceed.
 */
@Component
public class CatalogClient {

    private static final Logger log =
            LoggerFactory.getLogger(CatalogClient.class);

    private final RestClient restClient;

    public CatalogClient(
            @Value("${catalog.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /** OPEN -> HELD. Throws if the slot is not available. */
    public void holdSlot(Long slotId) {
        transition(slotId, "hold");
    }

    /** HELD -> BOOKED. Throws if the slot is no longer held. */
    public void bookSlot(Long slotId) {
        transition(slotId, "book");
    }

    /**
     * HELD/BOOKED -> OPEN, best-effort. A failed release is logged, not thrown:
     * the user-facing action (decline/cancel) must still succeed, and the
     * catalog-side stale-hold sweep is the safety net.
     */
    public void releaseSlotQuietly(Long slotId) {
        try {
            restClient.post()
                    .uri("/api/slots/{id}/release", slotId)
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
                    .uri("/api/slots/{id}/{action}", slotId, action)
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

    /** Minimal projection of catalog's service for price snapshotting. */
    private record ServiceView(Long id, Double price) {
    }
}
