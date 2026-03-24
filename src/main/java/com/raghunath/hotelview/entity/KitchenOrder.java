package com.raghunath.hotelview.entity;

import com.raghunath.hotelview.dto.admin.OrderItem;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@CompoundIndex(name = "hotel_table_status", def = "{'hotelId': 1, 'tableNumber': 1, 'status': 1}")
@Document(collection = "kitchen_orders")
public class KitchenOrder {
    @Id
    private String id;
    private String hotelId; // INDEX REQUIRED
    private int tableNumber; // INDEX REQUIRED
    private List<OrderItem> items;
    private Double totalAmount;
    private String status; // "PENDING", "PREPARING", "COMPLETED", "PAID"

    private String createdBy; // Waiter/Admin ID
    private String acceptedBy; // Chef/Admin ID

    private LocalDateTime createdAt;
}