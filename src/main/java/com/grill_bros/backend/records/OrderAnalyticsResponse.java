package com.grill_bros.backend.records;

import java.util.List;

public record OrderAnalyticsResponse(
        List<AdminOrderStats> adminStats,
        List<PaymentMethodOrderCount> orderCounts,
        List<PaymentMethodRevenue> revenues
) {
}
