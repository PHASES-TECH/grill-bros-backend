package com.grill_bros.backend.records;

import java.math.BigDecimal;

public record CategoryRevenueDistribution(
        String category,
        BigDecimal revenue,
        double percentage
) {
}
