package com.example.catalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Generate bookable slots from working hours: for each day in
 * [fromDate, toDate], carve the window [dailyStart, dailyEnd) into fixed-length
 * slots of {@code slotMinutes}.
 */
@Getter
@Setter
public class SlotGenerationRequest {

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    @NotNull
    private LocalTime dailyStart;

    @NotNull
    private LocalTime dailyEnd;

    @NotNull
    @Positive
    private Integer slotMinutes;
}
