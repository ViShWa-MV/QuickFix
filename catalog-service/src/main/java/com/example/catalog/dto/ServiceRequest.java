package com.example.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** A bookable service (with its base price) as an admin submits it. */
@Getter
@Setter
public class ServiceRequest {

    @NotNull
    private Long categoryId;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    @PositiveOrZero
    private Double price;

    private boolean active = true;
}
