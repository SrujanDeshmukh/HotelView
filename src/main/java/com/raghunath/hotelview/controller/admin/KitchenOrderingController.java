package com.raghunath.hotelview.controller.admin;

import com.raghunath.hotelview.entity.KitchenOrder;
import com.raghunath.hotelview.repository.KitchenOrderRepository;
import com.raghunath.hotelview.service.admin.KitchenOrderingService;
import com.raghunath.hotelview.service.admin.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class KitchenOrderingController {

    private final KitchenOrderRepository kitchenOrderRepository;
    private final KitchenOrderingService kitchenOrderingService;

    private String getAuthenticatedUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }


    // 5. CHEF: FETCH PENDING ORDERS (New orders only)
    @GetMapping("/kitchen/pending")
    public ResponseEntity<List<KitchenOrder>> getPendingOrders() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(kitchenOrderRepository.findByHotelIdAndStatus(hotelId, "PENDING"));
    }

    // 6. CHEF: ACCEPT ORDER (Change PENDING -> PREPARING)
    @PatchMapping("/kitchen/accept/{orderId}")
    public ResponseEntity<String> acceptOrder(@PathVariable String orderId) {
        String chefId = getAuthenticatedUserId();
        kitchenOrderingService.updateStatusWithChef(orderId, "PREPARING", chefId);
        return ResponseEntity.ok("Accepted by " + chefId);
    }

    // 7. CHEF: FETCH PREPARING ORDERS (Orders currently being cooked)
    @GetMapping("/kitchen/preparing")
    public ResponseEntity<List<KitchenOrder>> getPreparingOrders() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(kitchenOrderRepository.findByHotelIdAndStatus(hotelId, "PREPARING"));
    }

    // 8. CHEF: COMPLETE ORDER (Change PREPARING -> COMPLETED)
    @PatchMapping("/kitchen/complete/{orderId}")
    public ResponseEntity<String> completeOrder(@PathVariable String orderId) {
        kitchenOrderingService.updateOrderStatus(orderId, "COMPLETED");
        return ResponseEntity.ok("Order marked as COMPLETED");
    }

    @GetMapping("/kitchen/completed")
    public ResponseEntity<List<KitchenOrder>> getCompletedOrders() {
        String hotelId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(kitchenOrderRepository.findByHotelIdAndStatus(hotelId, "COMPLETED"));
    }
}
