package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.OrderItem;
import com.raghunath.hotelview.entity.KitchenOrder;
import com.raghunath.hotelview.entity.OrderDraft;
import com.raghunath.hotelview.repository.KitchenOrderRepository;
import com.raghunath.hotelview.repository.OrderDraftRepository;
import com.raghunath.hotelview.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public String confirmOrder(String hotelId, int tableNumber) {
        // Find the active draft
        OrderDraft draft = draftRepository.findByHotelIdAndTableNumber(hotelId, tableNumber)
                .orElseThrow(() -> new RuntimeException("No active draft found for Table " + tableNumber));

        // Create the Kitchen Order Ticket (KOT)
        KitchenOrder kOrder = KitchenOrder.builder()
                .hotelId(hotelId)
                .tableNumber(tableNumber)
                .items(draft.getItems())
                .totalAmount(draft.getTotalAmount())
                .status("PENDING") // Initial status for Chef
                .orderTime(LocalDateTime.now())
                .build();

        kitchenOrderRepository.save(kOrder);

        // Cleanup: Delete the draft so it doesn't show up as 'unsaved' anymore
        draftRepository.delete(draft);

        // Update table to 'Order Received' status
        updateTableStatus(hotelId, tableNumber, "Order Received");

        return "Order sent to kitchen successfully!";
    }

    // Helper method for status updates
    private void updateTableStatus(String hotelId, int tableNumber, String status) {
        tableRepository.findByHotelIdAndTableNumber(hotelId, tableNumber).ifPresent(t -> {
            t.setStatus(status);
            tableRepository.save(t);
        });
    }
}