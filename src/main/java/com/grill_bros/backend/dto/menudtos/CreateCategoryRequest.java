package com.grill_bros.backend.dto.menudtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 100, message = "Slug must not exceed 100 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens only")
    private String slug;

    @Min(value = 0, message = "Display order must be non-negative")
    private int displayOrder = 0;
}
