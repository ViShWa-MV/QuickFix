package com.example.catalog.controller;

import com.example.catalog.dto.SlotGenerationRequest;
import com.example.catalog.dto.SlotRequest;
import com.example.catalog.dto.SlotResponse;
import com.example.catalog.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * A technician's availability slots.
 *
 * <p>Step 7 (hardening): these endpoints must be restricted to the owning
 * technician once catalog-service sits behind authentication; they are
 * currently open.
 */
@RestController
@RequestMapping("/api/technicians/{profileId}/slots")
public class AvailabilityController {

    private final AvailabilityService service;

    public AvailabilityController(AvailabilityService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse create(
            @PathVariable Long profileId,
            @Valid @RequestBody SlotRequest request) {
        return service.createSlot(profileId, request);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SlotResponse> generate(
            @PathVariable Long profileId,
            @Valid @RequestBody SlotGenerationRequest request) {
        return service.generate(profileId, request);
    }

    @GetMapping
    public List<SlotResponse> list(
            @PathVariable Long profileId) {
        return service.listSlots(profileId);
    }

    @DeleteMapping("/{slotId}")
    public String delete(
            @PathVariable Long profileId,
            @PathVariable Long slotId) {
        service.deleteSlot(profileId, slotId);
        return "Slot removed";
    }
}
