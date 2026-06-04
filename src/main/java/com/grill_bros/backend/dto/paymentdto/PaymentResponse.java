package com.grill_bros.backend.dto.paymentdto;

import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PaymentResponse {

    private String id;

    private String orderId;
    private String orderNumber;

    private PaymentStatus status;

    private String reference;
    private String accessCode;
    private String authorizationUrl;
    private String paystackTransactionId;

    private String customerEmail;
    private String customerPhone;

    private BigDecimal amount;
    private String currency;

    private String gatewayResponse;
    private String channel;

    private Instant initiatedAt;
    private Instant paidAt;
    private Instant completedAt;

    private Instant createdAt;
    private Instant updatedAt;

    public static PaymentResponse from(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .orderNumber(p.getOrder().getOrderNumber())
                .status(p.getStatus())
                .reference(p.getReference())
                .accessCode(p.getAccessCode())
                .authorizationUrl(p.getAuthorizationUrl())
                .paystackTransactionId(p.getPaystackTransactionId())
                .customerEmail(p.getCustomerEmail())
                .customerPhone(p.getCustomerPhone())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .gatewayResponse(p.getGatewayResponse())
                .channel(p.getChannel())
                .initiatedAt(p.getInitiatedAt())
                .paidAt(p.getPaidAt())
                .completedAt(p.getCompletedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public static PaymentResponse summary(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderNumber(p.getOrder().getOrderNumber())
                .status(p.getStatus())
                .reference(p.getReference())
                .customerPhone(p.getCustomerPhone())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .channel(p.getChannel())
                .initiatedAt(p.getInitiatedAt())
                .completedAt(p.getCompletedAt())
                .build();
    }
}
