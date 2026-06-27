package com.raghunath.hotelview.controller.admin;

import com.raghunath.hotelview.dto.admin.DeliverySummaryDTO;
import com.raghunath.hotelview.dto.admin.ReceiptResponse;
import com.raghunath.hotelview.dto.admin.SalesAnalyticsDTO;
import com.raghunath.hotelview.entity.CompletedOrder;
import com.raghunath.hotelview.service.admin.CompletedOrderService;
import com.raghunath.hotelview.service.admin.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final CompletedOrderService completedOrderService;

    @GetMapping("/list")
    public ResponseEntity<Page<DeliverySummaryDTO>> getCompletedList(
            @RequestParam(defaultValue = "0") int page) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(completedOrderService.getCompletedOrdersPaged(hotelId, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> getOrderDetails(@PathVariable String id) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(completedOrderService.getReceiptDetails(id, hotelId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CompletedOrder>> searchOrders(@RequestParam String query) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(completedOrderService.searchCompletedOrders(hotelId, query));
    }

    @GetMapping("/delivery/today")
    public ResponseEntity<List<DeliverySummaryDTO>> getTodayCompletedDeliveries() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(completedOrderService.getTodayCompletedHomeDeliveries(hotelId));
    }

    // --- EDIT COMPLETED ORDER DETAILS & TOTALS ---
    @PutMapping("/{id}/edit")
    public ResponseEntity<String> editCompletedOrder(
            @PathVariable String id,
            @RequestBody com.raghunath.hotelview.dto.admin.CompletedOrderEditDTO editDto) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        completedOrderService.editCompletedOrderDetails(hotelId, id, editDto);
        return ResponseEntity.ok("Completed order items quantity and totals updated successfully.");
    }

    // --- PERMANENTLY DELETE COMPLETED ORDER ---
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCompletedOrder(@PathVariable String id) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        completedOrderService.deleteCompletedOrderById(hotelId, id);
        return ResponseEntity.ok("Completed order deleted successfully and customer tracking reversed.");
    }

    @DeleteMapping("/delete-multiple")
    public ResponseEntity<String> deleteOrders(@RequestBody List<String> orderIds) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        completedOrderService.softDeleteOrders(hotelId, orderIds);
        return ResponseEntity.ok("Orders moved to trash successfully.");
    }

    @GetMapping("/analytics/today")
    @Cacheable(value = "orderCache", key = "#principal.name + '-today'")
    public ResponseEntity<SalesAnalyticsDTO> getTodayAnalytics(java.security.Principal principal) { // 👈 Injected Principal parameter
        String hotelId = principal.getName(); // 👈 Bypasses manual SecurityContextHolder lookups!
        return ResponseEntity.ok(orderService.getTodayHourlySales(hotelId));
    }

    @GetMapping("/analytics/week")
    @Cacheable(value = "orderCache", key = "#principal.name + '-week'")
    public ResponseEntity<SalesAnalyticsDTO> getWeeklyAnalytics(java.security.Principal principal) {
        String hotelId = principal.getName();
        return ResponseEntity.ok(orderService.getCurrentWeekSales(hotelId));
    }

    @GetMapping("/analytics/month")
    @Cacheable(value = "orderCache", key = "#principal.name + '-month'")
    public ResponseEntity<SalesAnalyticsDTO> getMonthlyAnalytics(java.security.Principal principal) {
        String hotelId = principal.getName();
        return ResponseEntity.ok(orderService.getCurrentMonthSales(hotelId));
    }

    @GetMapping("/analytics/year")
    @Cacheable(value = "orderCache", key = "#principal.name + '-year'")
    public ResponseEntity<SalesAnalyticsDTO> getYearlyAnalytics(java.security.Principal principal) {
        String hotelId = principal.getName();
        return ResponseEntity.ok(orderService.getCurrentYearSales(hotelId));
    }

    @GetMapping("/deleted-list")
    public ResponseEntity<List<org.bson.Document>> getDeletedOrders() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(completedOrderService.getDeletedOrders(hotelId));
    }
}