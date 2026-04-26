package com.grill_bros.backend.dto.menudtos;

import com.grill_bros.backend.model.MenuItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MenuItemResponse {

    private UUID        id;
    private String      name;
    private String      description;
    private BigDecimal  price;
    private String      imageUrl;
    private boolean     available;
    private boolean     active;
    private int         sortOrder;
    private List<String> tags;
    private UUID        categoryId;
    private String      categoryName;
    private Instant     createdAt;
    private Instant     updatedAt;

    public static MenuItemResponse from(MenuItem m) {
        return MenuItemResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .description(m.getDescription())
                .price(m.getPrice())
                .imageUrl(m.getImageUrl())
                .available(m.isAvailable())
                .active(m.isActive())
                .sortOrder(m.getSortOrder())
                .tags(m.getTags())
                .categoryId(m.getCategory().getId())
                .categoryName(m.getCategory().getName())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
