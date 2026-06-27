package com.raghunath.hotelview.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class CompletedOrderEditDTO {
    private Double discountPercent;     // Optional override
    private Double customTotalPayable;  // Optional manual override (e.g., custom round-off)
    private List<UpdatedItemPayload> items;

    @Data
    public static class UpdatedItemPayload {
        private String itemName;
        private int quantity;
    }
}