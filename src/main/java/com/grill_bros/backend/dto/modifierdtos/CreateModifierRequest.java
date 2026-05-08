package com.grill_bros.backend.dto.modifierdtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateModifierRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private String price;

    @NotNull
    private UUID groupId;
}
