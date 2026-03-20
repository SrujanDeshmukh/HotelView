package com.raghunath.hotelview.entity;

import com.raghunath.hotelview.dto.admin.OrderItem;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Document(collection = "kitchen_orders")
public class KitchenOrder {
    @Id
    private String id;
    private String hotelId;
    private int tableNumber;
    private List<OrderItem> items;
    private Double totalAmount;
    private String status; // "PENDING", "PREPARING", "READY", "SERVED"
    private LocalDateTime orderTime;
}