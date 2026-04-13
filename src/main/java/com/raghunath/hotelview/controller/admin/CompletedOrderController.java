package com.raghunath.hotelview.controller.admin;

import com.raghunath.hotelview.dto.admin.DeliverySummaryDTO;
import com.raghunath.hotelview.dto.admin.ReceiptResponse;
import com.raghunath.hotelview.dto.admin.SalesAnalyticsDTO;
import com.raghunath.hotelview.entity.CompletedOrder;
import com.raghunath.hotelview.service.admin.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/completed")
@RequiredArgsConstructor
public class CompletedOrderController {

    private final OrderService orderService;

    @GetMapping("/list")
    public ResponseEntity<Page<DeliverySummaryDTO>> getCompletedList(
            @RequestParam(defaultValue = "0") int page) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getCompletedOrdersPaged(hotelId, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> getOrderDetails(@PathVariable String id) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getReceiptDetails(id, hotelId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CompletedOrder>> searchOrders(@RequestParam String query) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.searchCompletedOrders(hotelId, query));
    }

    @GetMapping("/delivery/today")
    public ResponseEntity<List<DeliverySummaryDTO>> getTodayCompletedDeliveries() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getTodayCompletedHomeDeliveries(hotelId));
    }

    // API 1: Move specific orders to deleted collection
    @DeleteMapping("/delete-multiple")
    public ResponseEntity<String> deleteOrders(@RequestBody List<String> orderIds) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        orderService.softDeleteOrders(hotelId, orderIds);
        return ResponseEntity.ok("Orders moved to trash successfully.");
    }

    @GetMapping("/deleted-list")
    public ResponseEntity<List<org.bson.Document>> getDeletedOrders() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getDeletedOrders(hotelId));
    }

    // Inside CompletedOrderController.java

    @GetMapping("/analytics/today")
    public ResponseEntity<SalesAnalyticsDTO> getTodayAnalytics() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getTodayHourlySales(hotelId));
    }

    @GetMapping("/analytics/week")
    public ResponseEntity<SalesAnalyticsDTO> getWeeklyAnalytics() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getCurrentWeekSales(hotelId));
    }

    @GetMapping("/analytics/month")
    public ResponseEntity<SalesAnalyticsDTO> getMonthlyAnalytics() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getCurrentMonthSales(hotelId));
    }

    @GetMapping("/analytics/year")
    public ResponseEntity<SalesAnalyticsDTO> getYearlyAnalytics() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getCurrentYearSales(hotelId));
    }
}