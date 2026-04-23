package com.grill_bros.backend.dto.dashboarddtos;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class DashboardStatsResponse {

    private long       todayOrderCount;
    private BigDecimal todayRevenue;
    private BigDecimal avgOrderValue;
    private long       pendingOrders;
    private long       completedOrders;

    private Map<String, Long> ordersByStatus;
}
