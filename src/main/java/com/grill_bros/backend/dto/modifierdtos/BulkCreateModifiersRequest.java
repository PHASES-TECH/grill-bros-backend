package com.grill_bros.backend.dto.modifierdtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkCreateModifiersRequest {

    @NotNull
    private UUID groupId;

    @NotEmpty
    private List<CreateModifierRequest> modifiers;
}
