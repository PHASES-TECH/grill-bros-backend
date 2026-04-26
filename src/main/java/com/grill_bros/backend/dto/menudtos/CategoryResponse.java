package com.grill_bros.backend.dto.menudtos;

import com.grill_bros.backend.model.MenuCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CategoryResponse {

    private UUID    id;
    private String  name;
    private String  slug;
    private int     displayOrder;
    private boolean active;
    private Instant createdAt;

    public static CategoryResponse from(MenuCategory c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .displayOrder(c.getDisplayOrder())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
