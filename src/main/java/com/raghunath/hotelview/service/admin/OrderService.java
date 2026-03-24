package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.OrderItem;
import com.raghunath.hotelview.entity.KitchenOrder;
import com.raghunath.hotelview.entity.OrderDraft;
import com.raghunath.hotelview.repository.KitchenOrderRepository;
import com.raghunath.hotelview.repository.OrderDraftRepository;
import com.raghunath.hotelview.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderDraftRepository draftRepository;
    private final TableRepository tableRepository;
    private final KitchenOrderRepository kitchenOrderRepository;

    /**
     * 1. SAVE DRAFT: Waiter/Admin adds items.
     * Since the waiter has full access, this updates the live table total immediately.
     */
    public void saveDraft(String hotelId, int tableNumber, List<OrderItem> items) {
        validateTableExists(hotelId, tableNumber);
        Double total = items.stream().mapToDouble(OrderItem::getSubTotal).sum();

        OrderDraft draft = draftRepository.findByHotelIdAndTableNumber(hotelId, tableNumber)
                .orElse(OrderDraft.builder()
                        .hotelId(hotelId)
                        .tableNumber(tableNumber)
                        .build());

        draft.setItems(items);
        draft.setTotalAmount(total);
        draft.setUpdatedAt(LocalDateTime.now());
        draftRepository.save(draft);
    }

    private void validateTableExists(String hotelId, int tableNumber) {
        // Check if a table with this number exists for this specific hotel
        boolean exists = tableRepository.existsByHotelIdAndTableNumber(hotelId, tableNumber);

        if (!exists) {
            log.error("VALIDATION_FAILED: Table {} does not exist for Hotel {}", tableNumber, hotelId);
            throw new RuntimeException("Invalid Table Number: " + tableNumber);
        }
    }

    /**
     * 2. FETCH DRAFT: View current unsent items.
     */
    public OrderDraft getDraft(String hotelId, int tableNumber) {
        return draftRepository.findByHotelIdAndTableNumber(hotelId, tableNumber).orElse(null);
    }

    /**
     * 3. CONFIRM ORDER: Moves items to the Kitchen.
     * Table status becomes 'PENDING' to alert the Chef.
     */
    @Transactional
    public String confirmOrder(String hotelId, int tableNumber, List<OrderItem> items, String waiterId) {
        validateTableExists(hotelId, tableNumber);
        log.info("CONFIRM_ORDER: Hotel: {}, Table: {}, Waiter: {}, Items: {}",
                hotelId, tableNumber, waiterId, items.size());
        Double total = items.stream().mapToDouble(OrderItem::getSubTotal).sum();

        KitchenOrder kOrder = KitchenOrder.builder()
                .hotelId(hotelId)
                .tableNumber(tableNumber)
                .items(items)
                .totalAmount(total)
                .status("PENDING")
                .createdBy(waiterId) // Audit Trail
                .createdAt(LocalDateTime.now())
                .build();

        kitchenOrderRepository.save(kOrder);
        draftRepository.findByHotelIdAndTableNumber(hotelId, tableNumber)
                .ifPresent(draftRepository::delete);

        updateTableVisualStatus(hotelId, tableNumber, "PENDING");
        log.info("ORDER_SUCCESS: Table {} is now PENDING", tableNumber);
        return "Order sent to kitchen";

    }
    /**
     * 4. FETCH TABLE ORDERS: Latest orders first.
     */
    public List<KitchenOrder> getOrdersByTable(String hotelId, int tableNumber) {
        return kitchenOrderRepository.findByHotelIdAndTableNumberAndStatusNotOrderByCreatedAtDesc(
                hotelId, tableNumber, "PAID");
    }

    /**
     * 5. UPDATE STATUS WITH CHEF: Handles Kitchen Lifecycle.
     * PREPARING -> Table shows 'PREPARING'
     * COMPLETED -> Table shows 'ACTIVE' (Food is served/Guest eating)
     */
    public void updateStatusWithChef(String orderId, String newStatus, String userId) {
        KitchenOrder order = kitchenOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // If the order is already COMPLETED, don't allow PREPARING (Safety Check)
        if ("COMPLETED".equals(order.getStatus()) && "PREPARING".equals(newStatus)) {
            throw new RuntimeException("Cannot move completed order back to preparing");
        }

        order.setStatus(newStatus.toUpperCase());

        if ("PREPARING".equalsIgnoreCase(newStatus)) {
            order.setAcceptedBy(userId); // Audit Trail: Who is cooking?
        }

        kitchenOrderRepository.save(order);

        // Map Table Visuals
        String tableUIStatus = "COMPLETED".equalsIgnoreCase(newStatus) ? "ACTIVE" : newStatus.toUpperCase();
        updateTableVisualStatus(order.getHotelId(), order.getTableNumber(), tableUIStatus);
    }

    /**
     * 6. GENERAL STATUS UPDATE: Fallback for direct status changes.
     */
    public void updateOrderStatus(String orderId, String newStatus) {
        updateStatusWithChef(orderId, newStatus, null);
    }

    /**
     * HELPER: Syncs the physical Table entity with the digital order status.
     */
    private void updateTableVisualStatus(String hotelId, int tableNumber, String status) {
        tableRepository.findByHotelIdAndTableNumber(hotelId, tableNumber).ifPresent(t -> {
            t.setStatus(status);
            tableRepository.save(t);
        });
    }
}