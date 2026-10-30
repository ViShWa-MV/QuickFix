package com.example.catalog.dto;

import com.example.catalog.entity.AvailabilitySlot;
import com.example.catalog.entity.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SlotResponse {

    private Long id;
    private Long technicianProfileId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private SlotStatus status;

    public static SlotResponse from(AvailabilitySlot s) {
        return new SlotResponse(
                s.getId(),
                s.getTechnicianProfileId(),
                s.getStartAt(),
                s.getEndAt(),
                s.getStatus());
    }
}
