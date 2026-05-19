package com.grill_bros.backend.dto.ordersdto;

import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.records.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class OrderResponse {

    private String        id;
    private String      orderNumber;
    private String      customerName;
    private String      customerPhone;
    private String      customerEmail;
    private OrderStatus status;
    private BigDecimal  subtotal;
    private BigDecimal  totalAmount;
    private String      notes;
    private String      placedByAdminName;
    private List<OrderItemResponse> items;
    private String trackingToken;
    private Instant     createdAt;
    private Instant     updatedAt;

    public static OrderResponse from(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .customerName(o.getCustomerName())
                .customerPhone(o.getCustomerPhone())
                .customerEmail(o.getCustomerEmail())
                .status(o.getStatus())
                .subtotal(o.getSubtotal())
                .totalAmount(o.getTotalAmount())
                .notes(o.getNotes())
                .placedByAdminName(o.getPlacedByAdmin() != null
                        ? o.getPlacedByAdmin().getFullName() : null)
                .items(o.getItems().stream()
                        .map(OrderItemResponse::from)
                        .collect(Collectors.toList()))
                .trackingToken(o.getTrackingToken())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    public static OrderResponse summary(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .customerName(o.getCustomerName())
                .customerPhone(o.getCustomerPhone())
                .status(o.getStatus())
                .totalAmount(o.getTotalAmount())
                .trackingToken(o.getTrackingToken())
                .placedByAdminName(o.getPlacedByAdmin() != null
                        ? o.getPlacedByAdmin().getFullName() : null)
                .createdAt(o.getCreatedAt())
                .build();
    }
}
