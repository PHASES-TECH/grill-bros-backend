package com.grill_bros.backend.dto.ordersdto;

import com.grill_bros.backend.records.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 200)
    private String customerName;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^0(24|25|53|54|55|59|20|50|26|56|27|57|28|58)\\d{7}$",
            message = "Invalid Ghanaian phone number"
    )
    private String customerPhone;

    @NotBlank(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Email(message = "Invalid email address")
    @Size(max = 255)
    private String customerEmail;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private Set<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {

        @NotNull(message = "Menu item ID is required")
        private UUID menuItemId;

        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 50, message = "Quantity cannot exceed 50")
        private int quantity;

        private List<UUID> modifierIds;
    }
}
