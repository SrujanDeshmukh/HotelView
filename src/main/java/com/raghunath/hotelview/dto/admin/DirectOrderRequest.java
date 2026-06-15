package com.raghunath.hotelview.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DirectOrderRequest {
        private String customerName;
        private String customerMobile;
        private String customerAddress;
        private String orderType;       // "TABLE", "TAKEAWAY", "DELIVERY"
        private String tableName;       // e.g., "Table 1", "Counter", or null
        private String itemName;        // "Deshmukh Order - special thali"
        private Integer quantity;
        private Double totalAmount;     // Total lump-sum value (e.g., 15000.0)
        private Double discount;        // Optional custom discount percentage
}
