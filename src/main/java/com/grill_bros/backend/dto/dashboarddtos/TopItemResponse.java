package com.grill_bros.backend.dto.dashboarddtos;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;


@Data
@Builder
public class TopItemResponse {

    private UUID       menuItemId;
    private String     itemName;
    private long       totalQuantitySold;
    private BigDecimal totalRevenue;

}
