package com.raghunath.hotelview.controller.admin;

import com.raghunath.hotelview.dto.admin.OrderItem;
import com.raghunath.hotelview.entity.KitchenOrder;
import com.raghunath.hotelview.entity.OrderDraft;
import com.raghunath.hotelview.repository.KitchenOrderRepository;
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
    private KitchenOrderRepository kitchenOrderRepository;

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

    // 1. PLACE ORDER (Waiter/Admin clicks 'Confirm')
    @PostMapping("/confirm/{tableNumber}")
    public ResponseEntity<String> confirmOrder(@PathVariable int tableNumber) {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        String message = orderService.confirmOrder(hotelId, tableNumber);
        return ResponseEntity.ok(message);
    }

    // 2. KITCHEN FEED (Chef/Admin sees what to cook)
    @GetMapping("/kitchen/live")
    public ResponseEntity<List<KitchenOrder>> getLiveOrders() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        // Standard: Fetch PENDING (New) and PREPARING (Accepted)
        List<KitchenOrder> orders = kitchenOrderRepository.findByHotelIdAndStatusIn(
                hotelId, List.of("PENDING", "PREPARING"));
        return ResponseEntity.ok(orders);
    }

    // 3. STATUS UPDATE (Chef marks as 'PREPARING' or 'COMPLETED')
    @PatchMapping("/kitchen/status/{orderId}")
    public ResponseEntity<String> updateStatus(@PathVariable String orderId, @RequestParam String status) {
        // Validation: Only allow specific transitions for 1-lakh user stability
        if (!List.of("PREPARING", "COMPLETED", "SERVED").contains(status.toUpperCase())) {
            return ResponseEntity.badRequest().body("Invalid Status");
        }

        orderService.updateOrderStatus(orderId, status.toUpperCase());
        return ResponseEntity.ok("Order " + orderId + " is now " + status);
    }
}
