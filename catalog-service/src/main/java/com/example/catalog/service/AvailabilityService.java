package com.example.catalog.service;

import com.example.catalog.dto.SlotGenerationRequest;
import com.example.catalog.dto.SlotRequest;
import com.example.catalog.dto.SlotResponse;
import com.example.catalog.entity.AvailabilitySlot;
import com.example.catalog.entity.SlotStatus;
import com.example.catalog.exception.ResourceNotFoundException;
import com.example.catalog.repository.AvailabilitySlotRepository;
import com.example.catalog.repository.TechnicianProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvailabilityService {

    /** Guard rails for bulk generation. */
    private static final long MAX_RANGE_DAYS = 62;
    private static final int MAX_GENERATED = 5000;

    private final AvailabilitySlotRepository slotRepository;
    private final TechnicianProfileRepository profileRepository;

    public AvailabilityService(
            AvailabilitySlotRepository slotRepository,
            TechnicianProfileRepository profileRepository) {
        this.slotRepository = slotRepository;
        this.profileRepository = profileRepository;
    }

    /** Publish a single slot. */
    public SlotResponse createSlot(Long profileId, SlotRequest request) {

        requireProfile(profileId);

        if (!request.getEndAt().isAfter(request.getStartAt())) {
            throw new IllegalArgumentException(
                    "endAt must be after startAt");
        }
        if (slotRepository.existsByTechnicianProfileIdAndStartAt(
                profileId, request.getStartAt())) {
            throw new IllegalStateException(
                    "A slot already starts at that time");
        }

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .technicianProfileId(profileId)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(SlotStatus.OPEN)
                .build();

        return SlotResponse.from(slotRepository.save(slot));
    }

    /** Generate fixed-length slots across a date range from working hours. */
    public List<SlotResponse> generate(
            Long profileId,
            SlotGenerationRequest request) {

        requireProfile(profileId);

        if (request.getToDate().isBefore(request.getFromDate())) {
            throw new IllegalArgumentException(
                    "toDate must not be before fromDate");
        }
        if (!request.getDailyEnd().isAfter(request.getDailyStart())) {
            throw new IllegalArgumentException(
                    "dailyEnd must be after dailyStart");
        }
        if (request.getFromDate().plusDays(MAX_RANGE_DAYS)
                .isBefore(request.getToDate())) {
            throw new IllegalArgumentException(
                    "Date range may not exceed " + MAX_RANGE_DAYS + " days");
        }

        List<AvailabilitySlot> toSave = new ArrayList<>();
        for (LocalDate day = request.getFromDate();
             !day.isAfter(request.getToDate());
             day = day.plusDays(1)) {

            LocalDateTime cursor = day.atTime(request.getDailyStart());
            LocalDateTime dayEnd = day.atTime(request.getDailyEnd());

            while (!cursor.plusMinutes(request.getSlotMinutes())
                    .isAfter(dayEnd)) {

                LocalDateTime start = cursor;
                LocalDateTime end = cursor.plusMinutes(request.getSlotMinutes());

                boolean duplicate = slotRepository
                        .existsByTechnicianProfileIdAndStartAt(profileId, start)
                        || toSave.stream()
                                .anyMatch(s -> s.getStartAt().equals(start));

                if (!duplicate) {
                    toSave.add(AvailabilitySlot.builder()
                            .technicianProfileId(profileId)
                            .startAt(start)
                            .endAt(end)
                            .status(SlotStatus.OPEN)
                            .build());
                }
                if (toSave.size() > MAX_GENERATED) {
                    throw new IllegalArgumentException(
                            "Too many slots requested; narrow the range or "
                            + "increase slotMinutes");
                }
                cursor = end;
            }
        }

        return slotRepository.saveAll(toSave).stream()
                .map(SlotResponse::from)
                .toList();
    }

    public List<SlotResponse> listSlots(Long profileId) {
        requireProfile(profileId);
        return slotRepository
                .findByTechnicianProfileIdOrderByStartAtAsc(profileId)
                .stream()
                .map(SlotResponse::from)
                .toList();
    }

    /** Remove an open slot (e.g. a day off). A held/booked slot cannot be removed here. */
    public void deleteSlot(Long profileId, Long slotId) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Slot not found"));

        if (!slot.getTechnicianProfileId().equals(profileId)) {
            throw new ResourceNotFoundException(
                    "Slot not found for this technician");
        }
        if (slot.getStatus() != SlotStatus.OPEN) {
            throw new IllegalStateException(
                    "Only OPEN slots can be removed");
        }
        slotRepository.delete(slot);
    }

    private void requireProfile(Long profileId) {
        if (!profileRepository.existsById(profileId)) {
            throw new ResourceNotFoundException(
                    "Technician profile not found");
        }
    }
}
