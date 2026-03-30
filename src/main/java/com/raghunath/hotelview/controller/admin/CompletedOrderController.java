package com.raghunath.hotelview.controller.admin;

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
     * API 1: Paged Summary List
     * Returns: ID, Name, Mobile, CheckoutAt, GrandTotal (5 per page)
     */
    @GetMapping("/list")
    public ResponseEntity<Page<CompletedOrder>> getCompletedList(
            @RequestParam(defaultValue = "0") int page) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getCompletedOrdersPaged(hotelId, page));
    }

    /**
     * API 2: Full Document Detail
     * Fetches everything (including items array) for a specific ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> getOrderDetails(@PathVariable String id) {
        // Fetch hotelId from the Access Token (Security Context)
        String hotelId = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        return ResponseEntity.ok(orderService.getReceiptDetails(id, hotelId));
    }

    /**
     * API 3: Search by Name or Mobile
     * Parameters: hotelId (from token), query (name or mobile)
     */
    @GetMapping("/search")
    public ResponseEntity<List<CompletedOrder>> searchOrders(@RequestParam String query) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.searchCompletedOrders(hotelId, query));
    }

}