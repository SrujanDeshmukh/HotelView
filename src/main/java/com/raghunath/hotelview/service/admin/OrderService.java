package com.raghunath.hotelview.service.admin;

import com.raghunath.hotelview.dto.admin.OrderItem;
import com.raghunath.hotelview.entity.OrderDraft;
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

    public void saveDraft(String hotelId, int tableNumber, List<OrderItem> items){
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

        // Update Table Status: If it was Vacant, now it is Occupied
        tableRepository.findByHotelIdAndTableNumber(hotelId, tableNumber).ifPresent(table -> {
            if ("Vacant".equals(table.getStatus())) {
                table.setStatus("Occupied");
                table.setCurrentBill(total);
                tableRepository.save(table);
            } else {
                // Just update the bill if already Occupied
                table.setCurrentBill(total);
                tableRepository.save(table);
            }
        });
    }

    public OrderDraft getDraft(String hotelId, int tableNumber) {
        return draftRepository.findByHotelIdAndTableNumber(hotelId, tableNumber)
                .orElse(null);
    }

    private void updateTableStatus(String hotelId, int tableNumber, String status){
        tableRepository.findByHotelIdAndTableNumber(hotelId, tableNumber).ifPresent(t -> {
            t.setStatus(status);
            tableRepository.save(t);
        });
    }
}
