package com.raghunath.hotelview.controller.admin;

import com.raghunath.hotelview.dto.admin.SalesAnalyticsDTO;
import com.raghunath.hotelview.service.admin.OrderAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/completed")
@RequiredArgsConstructor
public class OrderAnalyticsController {

    private final OrderAnalyticsService orderAnalyticsService;

    @GetMapping("/analytics/today")
    public ResponseEntity<SalesAnalyticsDTO> getTodayAnalytics() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderAnalyticsService.getTodayHourlySales(hotelId));
    }

    @GetMapping("/analytics/week")
    public ResponseEntity<SalesAnalyticsDTO> getWeeklyAnalytics() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderAnalyticsService.getCurrentWeekSales(hotelId));
    }

    @GetMapping("/analytics/month")
    public ResponseEntity<SalesAnalyticsDTO> getMonthlyAnalytics() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderAnalyticsService.getCurrentMonthSales(hotelId));
    }

    @GetMapping("/analytics/year")
    public ResponseEntity<SalesAnalyticsDTO> getYearlyAnalytics() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderAnalyticsService.getCurrentYearSales(hotelId));
    }
}
