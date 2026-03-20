package com.raghunath.hotelview.controller.admin;

import com.raghunath.hotelview.dto.admin.OrderItem;
import com.raghunath.hotelview.entity.OrderDraft;
import com.raghunath.hotelview.service.admin.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/draft/{tableNumber}")
    public ResponseEntity<String> saveOrderDraft(@PathVariable int tableNumber, @RequestBody List<OrderItem> items){
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        orderService.saveDraft(hotelId, tableNumber, items);
        return ResponseEntity.ok("Draft saved successfully");
    }

    @GetMapping("/draft/{tableNumber}")
    public OrderDraft getOrderDraft(@PathVariable int tableNumber){
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return orderService.getDraft(hotelId, tableNumber);
    }

    // Add to OrderController.java
    @PostMapping("/confirm/{tableNumber}")
    public ResponseEntity<String> confirmOrder(@PathVariable int tableNumber) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        String message = orderService.confirmOrder(hotelId, tableNumber);
        return ResponseEntity.ok(message);
    }
}
