package com.grill_bros.backend.records.paymentrecords;

public record PaymentinitiatedEvent(
        String paymentId,
        String orderId,
        String reference
) {
}
