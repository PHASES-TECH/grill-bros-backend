package com.grill_bros.backend.records;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminOrderStats(
        UUID adminId,
        String adminName,
        Long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue
) {
}
