package com.raghunath.hotelview.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomKitchenOrderRequest {
    private String tableName;
    private String orderType;     // "TABLE" or "PARCEL" (as seen in image_c49357.png)
    private String itemName;      // e.g., "Lays Chips - Blue"
    private Integer quantity;     // e.g., 2
    private Double subTotal;      // e.g., 40.0 (The total cost for these custom items)
    private String comment;       // Optional kitchen instructions
}
