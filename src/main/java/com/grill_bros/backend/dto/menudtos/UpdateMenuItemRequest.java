package com.grill_bros.backend.dto.menudtos;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateMenuItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @Size(max = 500)
    private String imageUrl;

    private int sortOrder;

    private List<String> tags;

    private boolean available;
}
