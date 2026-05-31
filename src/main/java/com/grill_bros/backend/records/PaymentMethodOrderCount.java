package com.grill_bros.backend.records;

public record PaymentMethodOrderCount(
        PaymentMethod paymentMethod,
        Long orderCount
) {
}
