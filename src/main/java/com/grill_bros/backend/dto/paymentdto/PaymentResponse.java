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

    private String        id;
    private Order order;
    private PaymentStatus status;
    private String      provider;
    private String      phoneNumber;
    private String momoReference;
    private Instant initiatedAt;
    private String  failureReason;
    private String      externalId;
    private String      currency;
    private BigDecimal amount;
    private Instant completedAt;
    private Instant createdAt;
    private Instant     updatedAt;

    public static PaymentResponse from(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .order(p.getOrder())
                .status(p.getStatus())
                .provider(p.getProvider())
                .phoneNumber(p.getPhoneNumber())
                .momoReference(p.getMomoReference())
                .initiatedAt(p.getInitiatedAt())
                .failureReason(p.getFailureReason())
                .externalId(p.getExternalId())
                .currency(p.getCurrency())
                .amount(p.getAmount())
                .completedAt(p.getCompletedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public static PaymentResponse summary(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .status(p.getStatus())
                .provider(p.getProvider())
                .phoneNumber(p.getPhoneNumber())
                .momoReference(p.getMomoReference())
                .initiatedAt(p.getInitiatedAt())
                .amount(p.getAmount())
                .completedAt(p.getCompletedAt())
                .build();
    }
}
