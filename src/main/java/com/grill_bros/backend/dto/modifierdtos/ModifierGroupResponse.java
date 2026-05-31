package com.grill_bros.backend.dto.modifierdtos;

import com.grill_bros.backend.model.ModifierGroup;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ModifierGroupResponse {

    private UUID id;
    private String name;
    private boolean required;
    private int minSelections;
    private int maxSelections;

    private UUID menuItemId;
    private String menuItemName;
    private List<ModifierResponse> modifiers;

    public static ModifierGroupResponse from(ModifierGroup group) {
        return ModifierGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .required(group.isRequired())
                .minSelections(group.getMinSelections())
                .maxSelections(group.getMaxSelections())
                .menuItemId(group.getMenuItem().getId())
                .menuItemName(group.getMenuItem().getName())
                .modifiers(group.getModifiers() != null
                        ? group.getModifiers().stream()
                        .map(ModifierResponse::from)
                        .toList()
                        : List.of())
                .build();
    }
}
