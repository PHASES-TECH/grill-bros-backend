package com.grill_bros.backend.dto.paymentdto;

import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.records.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PaymentStatusResponse {

    private String          paymentId;
    private String          orderId;
    private String        reference;
    private PaymentStatus paymentStatus;
    private BigDecimal    amount;
    private String        currency;
    private String        channel;           // card, mobile_money, bank
    private String        gatewayResponse;   // human-readable outcome
    private Instant paidAt;
    private Instant       initiatedAt;

    public static PaymentStatusResponse from(Payment p) {
        return PaymentStatusResponse.builder()
                .paymentId(p.getId())
                .orderId(p.getOrder().getId())
                .reference(p.getReference())
                .paymentStatus(p.getStatus())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .channel(p.getChannel())
                .gatewayResponse(p.getGatewayResponse())
                .paidAt(p.getPaidAt())
                .initiatedAt(p.getInitiatedAt())
                .build();
    }

}
