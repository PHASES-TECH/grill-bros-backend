package com.grill_bros.backend.dto.modifierdtos;

import com.grill_bros.backend.model.Modifier;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ModifierResponse {

    private UUID id;
    private String name;
    private BigDecimal price;
    private boolean active;
    private String groupName;
    private UUID groupId;

    public static ModifierResponse from(Modifier modifier) {
        return ModifierResponse.builder()
                .id(modifier.getId())
                .name(modifier.getName())
                .price(modifier.getPrice())
                .active(modifier.isActive())
                .groupId(modifier.getGroup().getId())
                .groupName(modifier.getGroup().getName())
                .build();
    }
}
