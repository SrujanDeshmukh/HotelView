package com.raghunath.hotelview.dto.admin;

import lombok.*;

@Getter @Setter @NoArgsConstructor
@AllArgsConstructor @Builder
public class OrderItem {
    private String itemName;
    private int quantity;
    private Double price;
    public Double getSubTotal() {
        if(this.price == null) return 0.0;
        return this.quantity * this.price;
    };
}
