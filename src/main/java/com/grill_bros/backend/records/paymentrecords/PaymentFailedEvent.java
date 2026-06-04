package com.grill_bros.backend.records.paymentrecords;

public record PaymentFailedEvent(
        String paymentId,
        String orderId,
        String reason
) {
}
