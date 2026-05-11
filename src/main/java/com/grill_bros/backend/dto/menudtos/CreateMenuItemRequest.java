package com.grill_bros.backend.dto.menudtos;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateMenuItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    private String price;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    private MultipartFile file;

    private int sortOrder = 0;

    private List<String> tags;
}
