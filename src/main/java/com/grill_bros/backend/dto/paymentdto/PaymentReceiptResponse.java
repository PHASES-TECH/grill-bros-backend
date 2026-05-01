package com.grill_bros.backend.dto.paymentdto;

import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.model.Receipt;
import com.grill_bros.backend.records.PaymentStatus;
import com.grill_bros.backend.records.ReceiptStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PaymentReceiptResponse {

    private String id;
    private Payment payment;
    private String orderId;
    private String reference;
    private ReceiptStatus status;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private BigDecimal amount;
    private String currency;
    private Instant issuedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static PaymentReceiptResponse from(Receipt receipt) {
        return PaymentReceiptResponse.builder()
                .id(receipt.getId())
                .payment(receipt.getPayment())
                .reference(receipt.getReference())
                .status(receipt.getStatus())
                .orderId(receipt.getOrderId())
                .customerName(receipt.getCustomerName())
                .customerEmail(receipt.getCustomerEmail())
                .customerPhone(receipt.getCustomerPhone())
                .amount(receipt.getAmount())
                .currency(receipt.getCurrency())
                .issuedAt(receipt.getIssuedAt())
                .createdAt(receipt.getCreatedAt())
                .updatedAt(receipt.getUpdatedAt())
                .build();
    }

    public static PaymentReceiptResponse summary(Receipt receipt) {
        return PaymentReceiptResponse.builder()
                .id(receipt.getId())
                .amount(receipt.getAmount())
                .currency(receipt.getCurrency())
                .customerName(receipt.getCustomerName())
                .customerEmail(receipt.getCustomerEmail())
                .customerPhone(receipt.getCustomerPhone())
                .issuedAt(receipt.getIssuedAt())
                .reference(receipt.getReference())
                .status(receipt.getStatus())
                .build();
    }
}
