package com.grill_bros.backend.dto.modifierdtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateModifierGroupRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private UUID menuItemId;

    private boolean required = false;

    @Min(0)
    private int minSelections = 0;

    @Min(1)
    private int maxSelections = 1;
}
