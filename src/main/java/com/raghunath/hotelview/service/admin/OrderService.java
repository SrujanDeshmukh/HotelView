package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.OrderItem;
import com.raghunath.hotelview.entity.KitchenOrder;
import com.raghunath.hotelview.entity.OrderDraft;
import com.raghunath.hotelview.repository.KitchenOrderRepository;
import com.raghunath.hotelview.repository.OrderDraftRepository;
import com.raghunath.hotelview.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderDraftRepository draftRepository;
    private final TableRepository tableRepository;
    private final KitchenOrderRepository kitchenOrderRepository;

    // 1. SAVE DRAFT (When Admin/Waiter adds items but hasn't confirmed yet)
    public void saveDraft(String hotelId, int tableNumber, List<OrderItem> items) {
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

        // Update Table Status to 'Occupied' if it was Vacant
        tableRepository.findByHotelIdAndTableNumber(hotelId, tableNumber).ifPresent(table -> {
            if ("Vacant".equals(table.getStatus())) {
                table.setStatus("Occupied");
            }
            table.setCurrentBill(total);
            tableRepository.save(table);
        });
    }

    // 2. FETCH DRAFT (When Waiter returns to a specific table)
    public OrderDraft getDraft(String hotelId, int tableNumber) {
        return draftRepository.findByHotelIdAndTableNumber(hotelId, tableNumber)
                .orElse(null);
    }

    // 3. CONFIRM ORDER (KOT Logic: Moves Draft to Kitchen)
    @Transactional // Ensures atomicity: Both save and delete happen, or neither does.
    public String confirmOrder(String hotelId, int tableNumber) {
        OrderDraft draft = draftRepository.findByHotelIdAndTableNumber(hotelId, tableNumber)
                .orElseThrow(() -> new RuntimeException("No active draft found for Table " + tableNumber));

        KitchenOrder kOrder = KitchenOrder.builder()
                .hotelId(hotelId)
                .tableNumber(tableNumber)
                .items(draft.getItems())
                .totalAmount(draft.getTotalAmount())
                .status("PENDING")
                .orderTime(LocalDateTime.now())
                .build();

        kitchenOrderRepository.save(kOrder);
        draftRepository.delete(draft);

        updateTableStatus(hotelId, tableNumber, "Order Received");
        return "Order sent to kitchen successfully!";
    }

    // 4. NEW: CHEF UPDATE API (To move order through lifecycle)
    public void updateOrderStatus(String orderId, String newStatus) {
        kitchenOrderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(newStatus);
            kitchenOrderRepository.save(order);

            // LOGIC: If order is COMPLETED, we might want to update the table status
            if ("COMPLETED".equals(newStatus)) {
                // Table remains 'Occupied' but waiter knows food is ready to be picked up
                updateTableStatus(order.getHotelId(), order.getTableNumber(), "Food Ready");
            }
            if ("SERVED".equals(newStatus)) {
                updateTableStatus(order.getHotelId(), order.getTableNumber(), "Occupied");
            }
        });
    }

    private void updateTableStatus(String hotelId, int tableNumber, String status) {
        tableRepository.findByHotelIdAndTableNumber(hotelId, tableNumber).ifPresent(t -> {
            t.setStatus(status);
            tableRepository.save(t);
        });
    }
}