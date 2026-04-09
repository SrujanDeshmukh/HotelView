package com.raghunath.hotelview.controller.admin;

import com.raghunath.hotelview.dto.admin.DeliverySummaryDTO;
import com.raghunath.hotelview.dto.admin.ReceiptResponse;
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

    /**
     * API 1: Paged Summary List (Archive)
     */
    @GetMapping("/list")
    public ResponseEntity<Page<DeliverySummaryDTO>> getCompletedList(
            @RequestParam(defaultValue = "0") int page) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getCompletedOrdersPaged(hotelId, page));
    }

    /**
     * API 2: Full Document Detail for Receipt/View
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> getOrderDetails(@PathVariable String id) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getReceiptDetails(id, hotelId));
    }

    /**
     * API 3: Search Archive by Name or Mobile
     */
    @GetMapping("/search")
    public ResponseEntity<List<CompletedOrder>> searchOrders(@RequestParam String query) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.searchCompletedOrders(hotelId, query));
    }

    /**
     * API 4: NEW - Fetch Today's Completed Home Deliveries (IST)
     * Fetches from completed_orders collection where type is HOME_DELIVERY
     */
    @GetMapping("/delivery/today")
    public ResponseEntity<List<DeliverySummaryDTO>> getTodayCompletedDeliveries() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getTodayCompletedHomeDeliveries(hotelId));
    }
}