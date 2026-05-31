package com.grill_bros.backend.records;

import java.math.BigDecimal;

public record PaymentMethodRevenue(
        PaymentMethod paymentMethod,
        BigDecimal revenue
) {
}
