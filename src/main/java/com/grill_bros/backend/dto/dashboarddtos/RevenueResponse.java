package com.grill_bros.backend.dto.dashboarddtos;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RevenueResponse {

    private BigDecimal          totalRevenue;
    private long                totalOrders;
    private BigDecimal          avgOrderValue;
    private List<DailyRevenue>  dailyBreakdown;

    @Data
    @Builder
    public static class DailyRevenue {

        private LocalDate  day;
        private long       orderCount;
        private BigDecimal revenue;

    }
}
