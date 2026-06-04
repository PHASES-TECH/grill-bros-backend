package com.grill_bros.backend.records.paymentrecords;

import java.math.BigDecimal;

public record PaymentSucceededEvent(
        String paymentId,
        String orderId,
        String orderNumber,
        String customerPhone,
        BigDecimal amount
) {
}
