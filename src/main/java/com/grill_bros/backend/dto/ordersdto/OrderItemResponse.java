package com.grill_bros.backend.dto.ordersdto;

import com.grill_bros.backend.model.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OrderItemResponse {

    private String       id;
    private UUID       menuItemId;
    private String     itemName;
    private BigDecimal unitPrice;
    private int        quantity;
    private BigDecimal lineTotal;

    public static OrderItemResponse from(OrderItem oi) {
        return OrderItemResponse.builder()
                .id(oi.getId())
                .menuItemId(oi.getMenuItem() != null ? oi.getMenuItem().getId() : null)
                .itemName(oi.getItemName())
                .unitPrice(oi.getUnitPrice())
                .quantity(oi.getQuantity())
                .lineTotal(oi.getLineTotal())
                .build();
    }
}