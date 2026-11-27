package com.example.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** A service category as an admin submits it. */
@Getter
@Setter
public class CategoryRequest {

    @NotBlank
    @Size(max = 80)
    private String name;

    @Size(max = 500)
    private String description;
}
