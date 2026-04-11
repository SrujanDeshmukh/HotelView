package com.raghunath.hotelview.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesAnalyticsDTO {
    private Double totalRevenue;
    private Long totalOrders;
    private Map<String, PeriodStats> chartData;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PeriodStats {
        private Double revenue;
        private Long orders;
    }
}