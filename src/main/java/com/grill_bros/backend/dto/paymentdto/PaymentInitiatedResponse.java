package com.grill_bros.backend.dto.paymentdto;

import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentInitiatedResponse {

    private String   paymentId;
    private String   orderId;
    private String reference;

    private String authorizationUrl;
    private String accessCode;

    private BigDecimal amount;
    private String        currency;
    private PaymentStatus status;

    public static PaymentInitiatedResponse from(Payment p) {
        return PaymentInitiatedResponse.builder()
                .paymentId(p.getId())
                .orderId(p.getOrder().getId())
                .reference(p.getReference())
                .authorizationUrl(p.getAuthorizationUrl())
                .accessCode(p.getAccessCode())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .build();
    }

}
