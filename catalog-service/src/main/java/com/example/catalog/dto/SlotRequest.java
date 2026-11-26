package com.example.catalog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** A single availability slot to publish. */
@Getter
@Setter
public class SlotRequest {

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;
}
