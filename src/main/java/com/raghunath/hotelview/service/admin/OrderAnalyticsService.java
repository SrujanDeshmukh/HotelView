package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.SalesAnalyticsDTO;
import com.raghunath.hotelview.repository.SalesAggregationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAnalyticsService {

    private final SalesAggregationRepository salesAggregationRepository;

    public SalesAnalyticsDTO getTodayHourlySales(String hotelId) {
        return salesAggregationRepository.getTodayAnalytics(hotelId);
    }

    public SalesAnalyticsDTO getCurrentWeekSales(String hotelId) {
        return salesAggregationRepository.getWeekAnalytics(hotelId);
    }

    public SalesAnalyticsDTO getCurrentMonthSales(String hotelId) {
        return salesAggregationRepository.getMonthAnalytics(hotelId);
    }

    public SalesAnalyticsDTO getCurrentYearSales(String hotelId) {
        return salesAggregationRepository.getYearAnalytics(hotelId);
    }
}
